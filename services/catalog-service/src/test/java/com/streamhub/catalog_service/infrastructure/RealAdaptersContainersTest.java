package com.streamhub.catalog_service.infrastructure;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole service against a real Redis and a real MongoDB. The Redis value is exactly what the
 * Recommendation Worker's HomeProjection writes; the titles have the shape of spec.md 2.3.
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

    @DynamicPropertySource
    static void connections(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("streamhub"));
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private MongoTemplate mongo;

    @BeforeEach
    void seed() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
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
    void aViewWrittenByTheWorkerIsAnsweredFromTheCacheAndCompletedWithTrending() throws Exception {
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
    void aProfileWithoutViewGetsTheFallbackFromMongoDb() throws Exception {
        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer("prof_0000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.categories[0].titles[0].content_id").value("mov_1"));
    }

    @Test
    void anUnreadableViewIsTreatedLikeAMissingOne() throws Exception {
        redis.opsForValue().set("home:prof_bad", "{this is not json");

        mvc.perform(get("/api/v1/home").header(HttpHeaders.AUTHORIZATION, bearer("prof_bad")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("fallback"));
    }
}
