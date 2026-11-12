package com.streamhub.recommendation_worker.infrastructure.adapters.out.catalog;

import com.streamhub.recommendation_worker.application.ports.out.CatalogGenresPort;
import com.streamhub.recommendation_worker.application.ports.out.CatalogGenresPort.CatalogUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CachingCatalogGenresAdapterTest {

    private static final Map<String, List<String>> GENRES = Map.of("mov_1", List.of("Drama"));

    /** A clock the test can move forward. */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-06T20:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static class CountingCatalog implements CatalogGenresPort {
        int calls;
        boolean down;

        @Override
        public Map<String, List<String>> genresByTitle() {
            calls++;
            if (down) {
                throw new CatalogUnavailableException("down", null);
            }
            return GENRES;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final CountingCatalog catalog = new CountingCatalog();
    private final CachingCatalogGenresAdapter adapter =
            new CachingCatalogGenresAdapter(catalog, clock, Duration.ofMinutes(5));

    @Test
    void theGenresAreAskedOnceWhileTheyAreFresh() {
        adapter.genresByTitle();
        clock.advance(Duration.ofMinutes(4));

        assertEquals(GENRES, adapter.genresByTitle());
        assertEquals(1, catalog.calls);
    }

    @Test
    void theyAreAskedAgainWhenTheyExpire() {
        adapter.genresByTitle();
        clock.advance(Duration.ofMinutes(5));
        adapter.genresByTitle();

        assertEquals(2, catalog.calls);
    }

    @Test
    void whenTheCatalogIsDownTheLastCopyIsUsed() {
        adapter.genresByTitle();
        clock.advance(Duration.ofMinutes(10));
        catalog.down = true;

        assertEquals(GENRES, adapter.genresByTitle());
    }

    @Test
    void withoutAnyCopyTheFailureIsPassedOn() {
        catalog.down = true;

        assertThrows(CatalogUnavailableException.class, adapter::genresByTitle);
    }
}
