package com.streamhub.catalog_service.infrastructure.adapters.out.mongo;

import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.TitleSummary;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrendingCachingTitleSummaryAdapterTest {

    private static final TitleSummary ONE = new TitleSummary("mov_1", "One", null);

    /** A clock the test can move forward. */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-06T20:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static class CountingCatalog implements TitleSummaryPort {
        int trendingCalls;
        int byIdsCalls;
        boolean down;

        @Override
        public List<TitleSummary> findByIds(List<String> titleIds) {
            byIdsCalls++;
            return List.of(ONE);
        }

        @Override
        public List<TitleSummary> findTrending(int limit) {
            trendingCalls++;
            if (down) {
                throw new ServiceUnavailableException("The catalog");
            }
            return List.of(ONE);
        }
    }

    private final MutableClock clock = new MutableClock();
    private final CountingCatalog catalog = new CountingCatalog();
    private final TrendingCachingTitleSummaryAdapter adapter =
            new TrendingCachingTitleSummaryAdapter(catalog, clock, Duration.ofSeconds(60));

    @Test
    void theTrendingTitlesAreQueriedOnceWhileTheyAreFresh() {
        adapter.findTrending(20);
        clock.advance(Duration.ofSeconds(59));
        List<TitleSummary> second = adapter.findTrending(20);

        assertEquals(List.of(ONE), second);
        assertEquals(1, catalog.trendingCalls);
    }

    @Test
    void theyAreQueriedAgainWhenTheyExpire() {
        adapter.findTrending(20);
        clock.advance(Duration.ofSeconds(60));
        adapter.findTrending(20);

        assertEquals(2, catalog.trendingCalls);
    }

    @Test
    void eachLimitHasItsOwnEntry() {
        adapter.findTrending(5);
        adapter.findTrending(20);

        assertEquals(2, catalog.trendingCalls);
    }

    @Test
    void aFailedQueryIsNotRemembered() {
        catalog.down = true;
        assertThrows(ServiceUnavailableException.class, () -> adapter.findTrending(20));

        catalog.down = false;
        assertEquals(List.of(ONE), adapter.findTrending(20));
        assertEquals(2, catalog.trendingCalls);
    }

    @Test
    void titlesByIdAreQueriedOnceWhileTheyAreFresh() {
        adapter.findByIds(List.of("mov_1"));
        clock.advance(Duration.ofSeconds(59));
        List<TitleSummary> second = adapter.findByIds(List.of("mov_1"));

        assertEquals(List.of(ONE), second);
        assertEquals(1, catalog.byIdsCalls);
    }

    @Test
    void titlesByIdAreQueriedAgainWhenTheyExpire() {
        adapter.findByIds(List.of("mov_1"));
        clock.advance(Duration.ofSeconds(60));
        adapter.findByIds(List.of("mov_1"));

        assertEquals(2, catalog.byIdsCalls);
    }
}
