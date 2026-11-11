package com.streamhub.catalog_service.infrastructure;

import org.bson.Document;
import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.time.Duration;
import java.util.List;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole service against real Redis, MongoDB and Cassandra containers.
 * Skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"jwt.secret=" + TestTokens.SECRET, "jwt.issuer=" + TestTokens.ISSUER})
@AutoConfigureMockMvc
class RealAdaptersContainersTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @Container
    static final GenericContainer<?> CASSANDRA = new GenericContainer<>("cassandra:4.1")
            .withExposedPorts(9042)
            .withStartupTimeout(Duration.ofMinutes(3));

    @DynamicPropertySource
    static void connections(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("streamhub"));
        registry.add("spring.cassandra.contact-points", CASSANDRA::getHost);
        registry.add("spring.cassandra.port", () -> CASSANDRA.getMappedPort(9042));
        registry.add("spring.cassandra.local-datacenter", () -> "datacenter1");
        registry.add("spring.cassandra.keyspace-name", () -> "streamhub");
    }

    @BeforeAll
    static void createInteractionsSchema() {
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(CASSANDRA.getHost(), CASSANDRA.getMappedPort(9042)))
                .withLocalDatacenter("datacenter1")
                .build()) {
            session.execute("""
                    CREATE KEYSPACE streamhub WITH replication =
                    {'class': 'SimpleStrategy', 'replication_factor': 1}
                    """);
            session.execute("""
                    CREATE TABLE streamhub.progress_by_profile (
                      profile_id text, content_id text, episode_id text, progress_seconds int,
                      last_event_timestamp timestamp, PRIMARY KEY ((profile_id), content_id))
                    """);
            session.execute("""
                    CREATE TABLE streamhub.recommendations_by_profile (
                      profile_id text PRIMARY KEY, content_ids list<text>, generated_at timestamp)
                    """);
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private MongoTemplate mongo;
    @Autowired
    private CqlSession cassandra;

    @BeforeEach
    void seed() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
        cassandra.execute("TRUNCATE streamhub.progress_by_profile");
        cassandra.execute("TRUNCATE streamhub.recommendations_by_profile");
        mongo.dropCollection("titles");
        title("mov_1", "Title One", 1);
        title("mov_2", "Title Two", 2);
        title("mov_77", "Title 77", null);
        redis.opsForValue().set("home:prof_9921", """
                {"profile_id":"prof_9921","generated_at":"2026-10-06T19:58:00Z","categories":[],
                 "continue_watching":[{"content_id":"mov_8832","episode_id":null,"progress_seconds":1420}],
                 "recommended":["mov_77","mov_gone"]}""");
    }

    private void title(String id, String name, Integer trendingRank) {
        Document document = new Document("_id", id).append("type", "MOVIE").append("name", name)
                .append("genres", List.of("Drama")).append("age_rating", "PG-13").append("duration_seconds", 5400)
                .append("images", new Document("poster", "https://cdn.example.com/p/" + id + ".jpg"));
        if (trendingRank != null) {
            document.append("trending_rank", trendingRank);
        }
        mongo.insert(document, "titles");
    }

    private static String bearer(String profileId) {
        return "Bearer " + TestTokens.profileToken(profileId);
    }

    @Test
    void aLegacyCachedViewIsAnsweredFromRedisAndCompletedWithTrending() throws Exception {
        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer("prof_9921")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("cache"))
                .andExpect(jsonPath("$.categories[0].name").value("Trending"))
                .andExpect(jsonPath("$.categories[0].titles.length()").value(2))
                .andExpect(jsonPath("$.categories[0].titles[0].content_id").value("mov_1"))
                .andExpect(jsonPath("$.categories[0].titles[1].content_id").value("mov_2"))
                .andExpect(jsonPath("$.recommended.length()").value(1))
                .andExpect(jsonPath("$.recommended[0].name").value("Title 77"))
                .andExpect(jsonPath("$.recommended[0].poster").value("https://cdn.example.com/p/mov_77.jpg"))
                .andExpect(jsonPath("$.continue_watching[0].content_id").value("mov_8832"))
                .andExpect(jsonPath("$.continue_watching[0].episode_id").doesNotExist());
    }

    @Test
    void aProfileWithoutViewRebuildsFromMongoAndCassandra() throws Exception {
        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer("prof_0000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.categories[0].titles[0].content_id").value("mov_1"));
    }

    @Test
    void aCacheMissRebuildsAndStoresContinueWatchingSeparately() throws Exception {
        cassandra.execute("""
                INSERT INTO streamhub.progress_by_profile
                  (profile_id, content_id, episode_id, progress_seconds, last_event_timestamp)
                VALUES ('prof_new', 'mov_8832', 'ep_102', 1420, '2026-10-06T19:58:00Z')
                """);
        cassandra.execute("""
                INSERT INTO streamhub.recommendations_by_profile (profile_id, content_ids, generated_at)
                VALUES ('prof_new', ['mov_77'], '2026-10-06T19:58:00Z')
                """);

        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer("prof_new")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.continue_watching[0].content_id").value("mov_8832"))
                .andExpect(jsonPath("$.recommended[0].content_id").value("mov_77"));

        org.junit.jupiter.api.Assertions.assertNotNull(redis.opsForValue().get("home:prof_new"));
        org.junit.jupiter.api.Assertions.assertTrue(redis.opsForValue().get("home:prof_new").indexOf("continue_watching") < 0);
        org.junit.jupiter.api.Assertions.assertTrue(redis.opsForValue().get("continue:prof_new").contains("mov_8832"));
        org.junit.jupiter.api.Assertions.assertTrue(redis.getExpire("home:prof_new", TimeUnit.SECONDS) > 0);
        org.junit.jupiter.api.Assertions.assertTrue(redis.getExpire("continue:prof_new", TimeUnit.SECONDS) > 0);
    }

    @Test
    void anUnreadableViewIsTreatedLikeAMissingOne() throws Exception {
        redis.opsForValue().set("home:prof_bad", "{this is not json");

        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer("prof_bad")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("fallback"));
    }
}
