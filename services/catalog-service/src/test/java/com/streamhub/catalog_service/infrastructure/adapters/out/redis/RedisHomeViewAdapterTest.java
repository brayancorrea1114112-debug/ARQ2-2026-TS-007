package com.streamhub.catalog_service.infrastructure.adapters.out.redis;

import com.streamhub.catalog_service.domain.model.CategoryRef;
import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;
import com.streamhub.catalog_service.domain.model.HomeView;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisHomeViewAdapterTest {

    @Test
    void storesHomeAndContinueWatchingUnderSeparateKeysWithJitteredTtls() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RedisHomeViewAdapter adapter = new RedisHomeViewAdapter(redis, Duration.ofMinutes(2));
        HomeView view = new HomeView("prof_1", Instant.parse("2026-10-06T19:58:00Z"),
                List.of(new CategoryRef("Trending", List.of("mov_1"))),
                List.of(new ContinueWatchingItem("mov_2", null, 300)), List.of("mov_3"));

        adapter.save(view, Duration.ofMinutes(15), Duration.ofHours(24));

        ArgumentCaptor<Duration> homeTtl = ArgumentCaptor.forClass(Duration.class);
        ArgumentCaptor<Duration> continueTtl = ArgumentCaptor.forClass(Duration.class);
        ArgumentCaptor<String> homeJson = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> continueWatchingJson = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq("home:prof_1"), homeJson.capture(), homeTtl.capture());
        verify(values).set(eq("continue:prof_1"), continueWatchingJson.capture(), continueTtl.capture());
        assertTrue(homeTtl.getValue().compareTo(Duration.ofMinutes(13)) >= 0);
        assertTrue(homeTtl.getValue().compareTo(Duration.ofMinutes(17)) <= 0);
        assertTrue(continueTtl.getValue().compareTo(Duration.ofHours(22)) >= 0);
        assertTrue(continueTtl.getValue().compareTo(Duration.ofHours(26)) <= 0);
        when(values.get("home:prof_1")).thenReturn(homeJson.getValue());
        when(values.get("continue:prof_1")).thenReturn(continueWatchingJson.getValue());

        HomeView cached = adapter.findByProfileId("prof_1").orElseThrow();

        assertEquals(view, cached);
        assertFalse(homeJson.getValue().contains("continue_watching"));
        assertTrue(continueWatchingJson.getValue().contains("mov_2"));
    }

    @Test
    void missesWhenOneOfTheSeparateCacheEntriesHasExpired() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("home:prof_1")).thenReturn("""
                {"generated_at":"2026-10-06T19:58:00Z","categories":[],"recommended":[]}""");

        assertTrue(new RedisHomeViewAdapter(redis, Duration.ZERO).findByProfileId("prof_1").isEmpty());
    }
}
