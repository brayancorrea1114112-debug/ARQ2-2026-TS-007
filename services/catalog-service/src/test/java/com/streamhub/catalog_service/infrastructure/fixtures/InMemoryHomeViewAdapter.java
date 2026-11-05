package com.streamhub.catalog_service.infrastructure.fixtures;

import com.streamhub.catalog_service.application.ports.out.HomeViewCachePort;
import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;
import com.streamhub.catalog_service.domain.model.HomeView;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One sample view, shaped like the ones the Recommendation Worker writes (no categories), for the tests that
 * do not need a real Redis. Any other profile has no view and gets the fallback.
 */
public class InMemoryHomeViewAdapter implements HomeViewCachePort {

    private final Map<String, HomeView> views = Map.of("prof_9921",
            new HomeView("prof_9921", Instant.parse("2026-10-06T19:58:00Z"), List.of(),
                    List.of(new ContinueWatchingItem("mov_8832", "ep_102", 1420)),
                    List.of("mov_77", "mov_91")));

    @Override
    public Optional<HomeView> findByProfileId(String profileId) {
        return Optional.ofNullable(views.get(profileId));
    }
}
