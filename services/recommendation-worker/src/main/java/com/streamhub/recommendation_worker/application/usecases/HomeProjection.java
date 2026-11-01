package com.streamhub.recommendation_worker.application.usecases;

import com.streamhub.recommendation_worker.application.ports.out.PreferenceStorePort;
import com.streamhub.recommendation_worker.application.ports.out.ProgressStorePort;
import com.streamhub.recommendation_worker.application.ports.out.RatingStorePort;
import com.streamhub.recommendation_worker.application.ports.out.RecommendationStrategy;
import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;
import com.streamhub.recommendation_worker.domain.model.RatingSnapshot;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Projects the durable Cassandra state into the disposable Redis read model
 * (keys defined in spec.md 2.4, 2.5 and 2.7).
 */
@Component
public class HomeProjection {

    static final Duration HOME_TTL = Duration.ofMinutes(15);
    static final Duration PROGRESS_TTL = Duration.ofHours(24);
    static final Duration PREFS_TTL = Duration.ofDays(7);
    private static final int CONTINUE_WATCHING_LIMIT = 20;

    private final RedisTemplate<String, Object> redis;
    private final ProgressStorePort progressStore;
    private final RatingStorePort ratingStore;
    private final PreferenceStorePort preferenceStore;
    private final RecommendationStrategy recommendationStrategy;

    public HomeProjection(RedisTemplate<String, Object> redis,
                          ProgressStorePort progressStore,
                          RatingStorePort ratingStore,
                          PreferenceStorePort preferenceStore,
                          RecommendationStrategy recommendationStrategy) {
        this.redis = redis;
        this.progressStore = progressStore;
        this.ratingStore = ratingStore;
        this.preferenceStore = preferenceStore;
        this.recommendationStrategy = recommendationStrategy;
    }

    public void projectProgress(String profileId) {
        List<ProgressSnapshot> progress = progressStore.findByProfile(profileId);
        List<RatingSnapshot> ratings = ratingStore.findByProfile(profileId);
        writeProgress(profileId, progress);
        writeHome(profileId, progress, ratings);
    }

    public void projectInteractions(String profileId) {
        List<RatingSnapshot> ratings = ratingStore.findByProfile(profileId);
        List<ProgressSnapshot> progress = progressStore.findByProfile(profileId);
        writePreferences(profileId, ratings);
        writeHome(profileId, progress, ratings);
    }

    public void projectFull(String profileId) {
        List<ProgressSnapshot> progress = progressStore.findByProfile(profileId);
        List<RatingSnapshot> ratings = ratingStore.findByProfile(profileId);
        writeProgress(profileId, progress);
        writePreferences(profileId, ratings);
        writeHome(profileId, progress, ratings);
    }

    private void writeProgress(String profileId, List<ProgressSnapshot> progress) {
        for (ProgressSnapshot snapshot : progress) {
            String key = "progress:" + profileId + ":" + snapshot.contentId();
            Map<String, Object> state = new HashMap<>();
            state.put("episode_id", snapshot.episodeId());
            state.put("progress_seconds", snapshot.progressSeconds());
            state.put("last_event_timestamp", snapshot.lastEventTimestamp().toString());
            redis.opsForHash().putAll(key, state);
            redis.expire(key, PROGRESS_TTL);
        }
    }

    private void writePreferences(String profileId, List<RatingSnapshot> ratings) {
        Map<String, Double> preferences = preferenceStore.findByProfile(profileId);
        if (preferences.isEmpty()) {
            preferences = derivePreferences(ratings);
        }
        if (preferences.isEmpty()) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        preferences.forEach(values::put);
        String key = "prefs:" + profileId;
        redis.opsForHash().putAll(key, values);
        redis.expire(key, PREFS_TTL);
    }

    private void writeHome(String profileId, List<ProgressSnapshot> progress, List<RatingSnapshot> ratings) {
        List<ProgressSnapshot> ordered = new ArrayList<>(progress);
        ordered.sort(Comparator.comparing(ProgressSnapshot::lastEventTimestamp).reversed());

        List<Map<String, Object>> continueWatching = ordered.stream()
                .limit(CONTINUE_WATCHING_LIMIT)
                .map(HomeProjection::toContinueWatching)
                .toList();

        List<String> recommended = recommendationStrategy.recommend(profileId, ratings, progress);

        Map<String, Object> homeView = new HashMap<>();
        homeView.put("profile_id", profileId);
        homeView.put("generated_at", Instant.now().toString());
        homeView.put("categories", List.of());
        homeView.put("continue_watching", continueWatching);
        homeView.put("recommended", recommended);

        redis.opsForValue().set("home:" + profileId, homeView, HOME_TTL);
    }

    private static Map<String, Object> toContinueWatching(ProgressSnapshot snapshot) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("content_id", snapshot.contentId());
        item.put("episode_id", snapshot.episodeId());
        item.put("progress_seconds", snapshot.progressSeconds());
        return item;
    }

    private static Map<String, Double> derivePreferences(List<RatingSnapshot> ratings) {
        Map<String, Double> preferences = new LinkedHashMap<>();
        for (RatingSnapshot rating : ratings) {
            preferences.put(rating.contentId(), "LIKE".equals(rating.rating()) ? 1.0 : -1.0);
        }
        return preferences;
    }
}
