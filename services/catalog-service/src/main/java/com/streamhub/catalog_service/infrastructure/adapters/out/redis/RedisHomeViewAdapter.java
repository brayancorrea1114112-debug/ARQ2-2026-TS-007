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
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Reads the precomputed Home view (key home:{profile_id}, spec.md 2.4) that the Recommendation Worker writes.
 * The value is read as plain text and parsed here, so this service does not depend on how the worker
 * serializes it: unknown fields are ignored and a null episode_id is accepted.
 */
public class RedisHomeViewAdapter implements HomeViewCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisHomeViewAdapter.class);
    private static final String KEY_PREFIX = "home:";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final StringRedisTemplate redis;

    public RedisHomeViewAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<HomeView> findByProfileId(String profileId) {
        String raw;
        try {
            raw = redis.opsForValue().get(KEY_PREFIX + profileId);
        } catch (DataAccessException e) {
            throw new ServiceUnavailableException("The cache", e);
        }
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(parse(profileId, raw));
        } catch (RuntimeException e) {
            // A view that cannot be read is as useless as a missing one: the fallback Home is better than an error.
            log.warn("Ignoring the unreadable Home view of {}: {}", profileId, e.getMessage());
            return Optional.empty();
        }
    }

    /** The profile id comes from the key, which is what was asked for, not from the document. */
    static HomeView parse(String profileId, String json) {
        ViewJson view = JSON.readValue(json, ViewJson.class);
        return new HomeView(profileId, Instant.parse(view.generatedAt()),
                view.categories() == null ? null : view.categories().stream()
                        .map(category -> new CategoryRef(category.name(), category.titleIds())).toList(),
                view.continueWatching() == null ? null : view.continueWatching().stream()
                        .map(item -> new ContinueWatchingItem(item.contentId(), item.episodeId(), item.progressSeconds())).toList(),
                view.recommended());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ViewJson(@JsonProperty("generated_at") String generatedAt,
                    List<CategoryJson> categories,
                    @JsonProperty("continue_watching") List<ContinueWatchingJson> continueWatching,
                    List<String> recommended) {
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
