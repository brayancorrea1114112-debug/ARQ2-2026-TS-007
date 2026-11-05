package com.streamhub.catalog_service.infrastructure.adapters.out.mongo;

import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.domain.model.TitleSummary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the trending titles in memory for a while. They are the same for every profile, and the Recommendation
 * Worker leaves every Home view without categories, so without this each cache hit would also query MongoDB.
 * A failed query is not remembered. Titles looked up by id are not cached: they depend on each profile.
 */
public class TrendingCachingTitleSummaryAdapter implements TitleSummaryPort {

    private record Entry(List<TitleSummary> titles, Instant expiresAt) {
    }

    private final TitleSummaryPort delegate;
    private final Clock clock;
    private final Duration ttl;
    private final Map<Integer, Entry> trendingByLimit = new ConcurrentHashMap<>();

    public TrendingCachingTitleSummaryAdapter(TitleSummaryPort delegate, Clock clock, Duration ttl) {
        this.delegate = delegate;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public List<TitleSummary> findByIds(List<String> titleIds) {
        return delegate.findByIds(titleIds);
    }

    @Override
    public List<TitleSummary> findTrending(int limit) {
        Instant now = clock.instant();
        Entry entry = trendingByLimit.get(limit);
        if (entry != null && now.isBefore(entry.expiresAt())) {
            return entry.titles();
        }
        List<TitleSummary> titles = List.copyOf(delegate.findTrending(limit));
        trendingByLimit.put(limit, new Entry(titles, now.plus(ttl)));
        return titles;
    }
}
