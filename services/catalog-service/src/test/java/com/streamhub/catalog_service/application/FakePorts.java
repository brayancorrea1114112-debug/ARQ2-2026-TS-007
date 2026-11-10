package com.streamhub.catalog_service.application;

import com.streamhub.catalog_service.application.ports.out.HomeViewCachePort;
import com.streamhub.catalog_service.application.ports.out.InteractionsReadPort;
import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;
import com.streamhub.catalog_service.domain.model.HomeView;
import com.streamhub.catalog_service.domain.model.TitleSummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;

/** In-memory replacements of the output ports, so the use case is tested without Redis or MongoDB. */
final class FakePorts {

    private FakePorts() {
    }

    static TitleSummary summary(String contentId) {
        return new TitleSummary(contentId, "Title " + contentId, "https://cdn.example.com/p/" + contentId + ".jpg");
    }

    static class InMemoryHomeViews implements HomeViewCachePort {

        private final Map<String, HomeView> views = new HashMap<>();
        boolean down;
        int saveCalls;

        void store(HomeView view) {
            views.put(view.profileId(), view);
        }

        @Override
        public Optional<HomeView> findByProfileId(String profileId) {
            if (down) {
                throw new ServiceUnavailableException("The cache");
            }
            return Optional.ofNullable(views.get(profileId));
        }

        @Override
        public void save(HomeView view, Duration homeTtl, Duration continueWatchingTtl) {
            if (down) {
                throw new ServiceUnavailableException("The cache");
            }
            saveCalls++;
            store(view);
        }
    }

    static class InMemoryInteractions implements InteractionsReadPort {

        private final Map<String, List<ContinueWatchingItem>> continueWatching = new HashMap<>();
        private final Map<String, List<String>> recommendations = new HashMap<>();
        boolean down;
        int continueWatchingCalls;
        int recommendationCalls;

        void store(String profileId, List<ContinueWatchingItem> watching, List<String> recommended) {
            continueWatching.put(profileId, watching);
            recommendations.put(profileId, recommended);
        }

        @Override
        public List<ContinueWatchingItem> findContinueWatching(String profileId) {
            continueWatchingCalls++;
            if (down) {
                throw new ServiceUnavailableException("The Interactions DB");
            }
            return continueWatching.getOrDefault(profileId, List.of());
        }

        @Override
        public List<String> findRecommendations(String profileId) {
            recommendationCalls++;
            if (down) {
                throw new ServiceUnavailableException("The Interactions DB");
            }
            return recommendations.getOrDefault(profileId, List.of());
        }
    }

    static class InMemoryTitles implements TitleSummaryPort {

        private final Map<String, TitleSummary> byId = new LinkedHashMap<>();
        private List<TitleSummary> trending = List.of();
        boolean down;
        int findByIdsCalls;
        int findTrendingCalls;
        List<String> lastRequestedIds = List.of();

        void add(String... contentIds) {
            for (String contentId : contentIds) {
                byId.put(contentId, summary(contentId));
            }
        }

        void trending(String... contentIds) {
            List<TitleSummary> summaries = new ArrayList<>();
            for (String contentId : contentIds) {
                TitleSummary title = summary(contentId);
                summaries.add(title);
                byId.put(contentId, title);
            }
            trending = summaries;
        }

        @Override
        public List<TitleSummary> findByIds(List<String> titleIds) {
            findByIdsCalls++;
            lastRequestedIds = titleIds;
            if (down) {
                throw new ServiceUnavailableException("The catalog");
            }
            return titleIds.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public List<TitleSummary> findTrending(int limit) {
            findTrendingCalls++;
            if (down) {
                throw new ServiceUnavailableException("The catalog");
            }
            return trending.stream().limit(limit).toList();
        }
    }
}
