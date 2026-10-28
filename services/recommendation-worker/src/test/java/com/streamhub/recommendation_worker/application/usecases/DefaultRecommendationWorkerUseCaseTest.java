package com.streamhub.recommendation_worker.application.usecases;

import com.streamhub.recommendation_worker.domain.model.PingReceivedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultRecommendationWorkerUseCaseTest {

    @Test
    void handlePing_acceptsNewEventAndRefreshesProgress() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        ListOperations<String, Object> listOps = mock(ListOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(valueOps.setIfAbsent("processed:evt-1", "1", java.time.Duration.ofHours(24))).thenReturn(true);
        when(hashOps.entries("progress:profile-1:content-1")).thenReturn(Collections.emptyMap());

        DefaultRecommendationWorkerUseCase useCase = new DefaultRecommendationWorkerUseCase(redisTemplate);
        PingReceivedEvent event = new PingReceivedEvent("evt-1", "profile-1", "content-1", "ep-101", 1200, Instant.parse("2026-10-06T20:00:00Z"));

        assertDoesNotThrow(() -> useCase.handlePing(event));

        verify(hashOps).putAll("progress:profile-1:content-1", Map.of(
                "episode_id", "ep-101",
                "progress_seconds", 1200,
                "last_event_timestamp", "2026-10-06T20:00:00Z"
        ));
    }

    @Test
    void handlePing_ignoresOutOfOrderEvent() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(valueOps.setIfAbsent("processed:evt-2", "1", java.time.Duration.ofHours(24))).thenReturn(true);
        when(hashOps.entries("progress:profile-2:content-2")).thenReturn(Map.of("last_event_timestamp", "2026-10-06T20:05:00Z"));

        DefaultRecommendationWorkerUseCase useCase = new DefaultRecommendationWorkerUseCase(redisTemplate);
        PingReceivedEvent event = new PingReceivedEvent("evt-2", "profile-2", "content-2", "ep-102", 1500, Instant.parse("2026-10-06T20:04:00Z"));

        useCase.handlePing(event);

        verify(hashOps, never()).putAll("progress:profile-2:content-2", Map.of(
                "episode_id", "ep-102",
                "progress_seconds", 1500,
                "last_event_timestamp", "2026-10-06T20:04:00Z"
        ));
    }
}
