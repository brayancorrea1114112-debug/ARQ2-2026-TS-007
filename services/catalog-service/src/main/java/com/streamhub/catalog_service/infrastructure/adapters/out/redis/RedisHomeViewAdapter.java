package com.streamhub.catalog_service.infrastructure.adapters.out.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.streamhub.catalog_service.application.ports.out.HomeViewCachePort;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.CategoryRef;
import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;
import com.streamhub.catalog_service.domain.model.HomeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stores and reads the cache-aside Home view. Continue Watching has its own key and TTL.
 */
public class RedisHomeViewAdapter implements HomeViewCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisHomeViewAdapter.class);
    private static final String HOME_KEY_PREFIX = "home:";
    private static final String CONTINUE_WATCHING_KEY_PREFIX = "continue:";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final StringRedisTemplate redis;
    private final Duration ttlJitter;

    public RedisHomeViewAdapter(StringRedisTemplate redis, Duration ttlJitter) {
        this.redis = redis;
        this.ttlJitter = ttlJitter;
    }

    @Override
    public Optional<HomeView> findByProfileId(String profileId) {
        String homeJson;
        String continueWatchingJson;
        try {
            homeJson = redis.opsForValue().get(HOME_KEY_PREFIX + profileId);
            if (homeJson == null) {
                return Optional.empty();
            }
            continueWatchingJson = redis.opsForValue().get(CONTINUE_WATCHING_KEY_PREFIX + profileId);
        } catch (DataAccessException e) {
            throw new ServiceUnavailableException("The cache", e);
        }
        try {
            ViewJson view = JSON.readValue(homeJson, ViewJson.class);
            List<ContinueWatchingJson> watching;
            if (continueWatchingJson == null) {
                if (view.continueWatching() == null) {
                    return Optional.empty();
                }
                watching = view.continueWatching();
            } else {
                watching = JSON.readValue(continueWatchingJson, ContinueWatchingListJson.class).items();
            }
            return Optional.of(toHomeView(profileId, view, watching));
        } catch (RuntimeException e) {
            log.warn("Ignoring the unreadable Home view of {}: {}", profileId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(HomeView view, Duration homeTtl, Duration continueWatchingTtl) {
        Map<String, Object> home = new LinkedHashMap<>();
        home.put("profile_id", view.profileId());
        home.put("generated_at", view.generatedAt().toString());
        home.put("categories", view.categories().stream().map(category -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("name", category.name());
            value.put("title_ids", category.titleIds());
            return value;
        }).toList());
        home.put("recommended", view.recommended());
        List<Map<String, Object>> continueWatching = new ArrayList<>();
        view.continueWatching().forEach(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("content_id", item.contentId());
            value.put("episode_id", item.episodeId());
            value.put("progress_seconds", item.progressSeconds());
            continueWatching.add(value);
        });

        try {
            redis.opsForValue().set(CONTINUE_WATCHING_KEY_PREFIX + view.profileId(),
                    JSON.writeValueAsString(Map.of("items", continueWatching)), randomized(continueWatchingTtl));
            redis.opsForValue().set(HOME_KEY_PREFIX + view.profileId(),
                    JSON.writeValueAsString(home), randomized(homeTtl));
        } catch (DataAccessException e) {
            throw new ServiceUnavailableException("The cache", e);
        } catch (JacksonException e) {
            throw new IllegalStateException("Could not serialize the Home cache entry", e);
        }
    }

    private Duration randomized(Duration ttl) {
        long jitterMillis = ttlJitter.toMillis();
        if (jitterMillis == 0) {
            return ttl;
        }
        long offset = ThreadLocalRandom.current().nextLong(-jitterMillis, jitterMillis + 1);
        Duration adjusted = ttl.plusMillis(offset);
        if (adjusted.isZero() || adjusted.isNegative()) {
            throw new IllegalArgumentException("TTL jitter must be smaller than the cache TTL");
        }
        return adjusted;
    }

    /** The profile id comes from the key, which is what was asked for, not from the document. */
    static HomeView parse(String profileId, String json) {
        ViewJson view = JSON.readValue(json, ViewJson.class);
        return toHomeView(profileId, view, view.continueWatching());
    }

    private static HomeView toHomeView(String profileId, ViewJson view, List<ContinueWatchingJson> watching) {
        return new HomeView(profileId, Instant.parse(view.generatedAt()),
                view.categories() == null ? null : view.categories().stream()
                        .map(category -> new CategoryRef(category.name(), category.titleIds())).toList(),
                watching == null ? null : watching.stream()
                        .map(item -> new ContinueWatchingItem(item.contentId(), item.episodeId(), item.progressSeconds())).toList(),
                view.recommended());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ViewJson(@JsonProperty("generated_at") String generatedAt,
                    List<CategoryJson> categories,
                    @JsonProperty("continue_watching") List<ContinueWatchingJson> continueWatching,
                    List<String> recommended) {
    }

    record ContinueWatchingListJson(List<ContinueWatchingJson> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CategoryJson(String name, @JsonProperty("title_ids") List<String> titleIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContinueWatchingJson(@JsonProperty("content_id") String contentId,
                                @JsonProperty("episode_id") String episodeId,
                                @JsonProperty("progress_seconds") int progressSeconds) {
    }
}
