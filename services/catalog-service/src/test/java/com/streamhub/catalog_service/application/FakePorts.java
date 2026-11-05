package com.streamhub.catalog_service.application;

import com.streamhub.catalog_service.application.ports.out.HomeViewCachePort;
import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.HomeView;
import com.streamhub.catalog_service.domain.model.TitleSummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                summaries.add(summary(contentId));
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
