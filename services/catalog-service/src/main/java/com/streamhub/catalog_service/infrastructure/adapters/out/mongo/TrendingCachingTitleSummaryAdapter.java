package com.streamhub.catalog_service.infrastructure.adapters.out.mongo;

import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.domain.model.TitleSummary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the trending titles and the title summaries in memory for a while. The Home views in Redis only hold
 * title ids, so without this every cache hit would also query MongoDB. A title that is not in memory is asked for
 * only once per expiration, and a failed query is not remembered.
 */
public class TrendingCachingTitleSummaryAdapter implements TitleSummaryPort {

    private record Entry(List<TitleSummary> titles, Instant expiresAt) {
    }

    private record TitleEntry(TitleSummary title, Instant expiresAt) {
    }

    private final TitleSummaryPort delegate;
    private final Clock clock;
    private final Duration ttl;
    private final Map<Integer, Entry> trendingByLimit = new ConcurrentHashMap<>();
    private final Map<String, TitleEntry> titlesById = new ConcurrentHashMap<>();

    public TrendingCachingTitleSummaryAdapter(TitleSummaryPort delegate, Clock clock, Duration ttl) {
        this.delegate = delegate;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public List<TitleSummary> findByIds(List<String> titleIds) {
        Instant now = clock.instant();
        List<TitleSummary> found = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String titleId : titleIds) {
            TitleEntry entry = titlesById.get(titleId);
            if (entry != null && now.isBefore(entry.expiresAt())) {
                found.add(entry.title());
            } else {
                missing.add(titleId);
            }
        }
        if (missing.isEmpty()) {
            return found;
        }
        List<TitleSummary> fetched = delegate.findByIds(missing);
        titlesById.values().removeIf(entry -> !now.isBefore(entry.expiresAt()));
        fetched.forEach(title -> titlesById.put(title.contentId(), new TitleEntry(title, now.plus(ttl))));
        found.addAll(fetched);
        return found;
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
