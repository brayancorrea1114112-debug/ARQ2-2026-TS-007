package com.streamhub.recommendation_worker.infrastructure.adapters.out.catalog;

import com.streamhub.recommendation_worker.application.ports.out.CatalogGenresPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Keeps the genres in memory for a while, so the worker does not call the catalog on every event. When the
 * catalog cannot be reached, the last copy is used even if it expired: genres rarely change. Only without any
 * copy is the failure passed on.
 */
public class CachingCatalogGenresAdapter implements CatalogGenresPort {

    private static final Logger log = LoggerFactory.getLogger(CachingCatalogGenresAdapter.class);

    private record Snapshot(Map<String, List<String>> genres, Instant fetchedAt) {
    }

    private final CatalogGenresPort delegate;
    private final Clock clock;
    private final Duration ttl;
    private volatile Snapshot snapshot;

    public CachingCatalogGenresAdapter(CatalogGenresPort delegate, Clock clock, Duration ttl) {
        this.delegate = delegate;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public Map<String, List<String>> genresByTitle() {
        Instant now = clock.instant();
        Snapshot current = snapshot;
        if (current != null && now.isBefore(current.fetchedAt().plus(ttl))) {
            return current.genres();
        }
        try {
            Map<String, List<String>> genres = delegate.genresByTitle();
            snapshot = new Snapshot(genres, now);
            return genres;
        } catch (CatalogUnavailableException e) {
            if (current == null) {
                throw e;
            }
            log.warn("The catalog is unavailable; using the genres fetched at {}", current.fetchedAt());
            return current.genres();
        }
    }
}
