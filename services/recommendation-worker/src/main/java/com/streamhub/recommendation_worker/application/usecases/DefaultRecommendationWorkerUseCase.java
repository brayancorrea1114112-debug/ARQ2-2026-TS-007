package com.streamhub.recommendation_worker.application.usecases;

import com.streamhub.recommendation_worker.application.ports.in.RecommendationWorkerUseCase;
import com.streamhub.recommendation_worker.domain.model.PingReceivedEvent;
import com.streamhub.recommendation_worker.domain.model.RatingAddedEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultRecommendationWorkerUseCase implements RecommendationWorkerUseCase {

    private static final Duration EVENT_TTL = Duration.ofHours(24);
    private static final Duration HOME_TTL = Duration.ofMinutes(15);

    private final RedisTemplate<String, Object> redisTemplate;

    public DefaultRecommendationWorkerUseCase(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void handlePing(PingReceivedEvent event) {
        if (event == null || event.eventId() == null || event.profileId() == null || event.contentId() == null) {
            throw new IllegalArgumentException("PingReceivedEvent payload is incomplete");
        }

        if (!isNewEvent(event.eventId())) {
            return;
        }

        String progressKey = "progress:" + event.profileId() + ":" + event.contentId();
        Map<Object, Object> current = redisTemplate.opsForHash().entries(progressKey);
        Object lastTimestampValue = current.get("last_event_timestamp");

        if (lastTimestampValue != null && Instant.parse(lastTimestampValue.toString()).isAfter(event.timestamp())) {
            markAsProcessed(event.eventId());
            return;
        }

        Map<String, Object> state = new HashMap<>();
        state.put("episode_id", event.episodeId());
        state.put("progress_seconds", event.progressSeconds());
        state.put("last_event_timestamp", event.timestamp().toString());
        redisTemplate.opsForHash().putAll(progressKey, state);
        redisTemplate.expire(progressKey, Duration.ofHours(24));

        Map<String, Object> continueWatching = new HashMap<>();
        continueWatching.put("content_id", event.contentId());
        continueWatching.put("episode_id", event.episodeId());
        continueWatching.put("progress_seconds", event.progressSeconds());
        redisTemplate.opsForList().rightPush("home:" + event.profileId() + ":continue_watching", continueWatching);
        refreshHomeView(event.profileId(), List.of(continueWatching));
        markAsProcessed(event.eventId());
    }

    @Override
    public void handleRating(RatingAddedEvent event) {
        if (event == null || event.eventId() == null || event.profileId() == null || event.contentId() == null) {
            throw new IllegalArgumentException("RatingAddedEvent payload is incomplete");
        }

        if (!isNewEvent(event.eventId())) {
            return;
        }

        String preferencesKey = "prefs:" + event.profileId();
        Map<Object, Object> existing = redisTemplate.opsForHash().entries(preferencesKey);
        Map<String, Object> next = new HashMap<>();
        for (Map.Entry<Object, Object> entry : existing.entrySet()) {
            next.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        next.put(event.contentId(), event.rating());
        redisTemplate.opsForHash().putAll(preferencesKey, next);
        redisTemplate.expire(preferencesKey, Duration.ofDays(7));

        List<String> recommendations = List.of("mov_77", "mov_91", event.contentId());
        refreshHomeView(event.profileId(), recommendations);
        markAsProcessed(event.eventId());
    }

    private boolean isNewEvent(String eventId) {
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent("processed:" + eventId, "1", EVENT_TTL);
        return Boolean.TRUE.equals(isNew);
    }

    private void markAsProcessed(String eventId) {
        redisTemplate.opsForValue().set("processed:" + eventId, "1", EVENT_TTL);
    }

    private void refreshHomeView(String profileId, List<?> continueWatching) {
        Map<String, Object> homeView = new HashMap<>();
        homeView.put("profile_id", profileId);
        homeView.put("generated_at", Instant.now().toString());
        homeView.put("continue_watching", continueWatching);
        homeView.put("recommended", List.of("mov_77", "mov_91", "mov_8832"));
        homeView.put("categories", List.of(Map.of("name", "Trending", "title_ids", List.of("mov_1", "mov_2", "mov_77", "mov_91"))));
        redisTemplate.opsForValue().set("home:" + profileId, homeView, HOME_TTL);
    }
}
