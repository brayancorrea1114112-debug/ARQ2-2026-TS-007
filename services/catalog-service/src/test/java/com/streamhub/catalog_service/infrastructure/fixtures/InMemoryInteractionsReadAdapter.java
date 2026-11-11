package com.streamhub.catalog_service.infrastructure.fixtures;

import com.streamhub.catalog_service.application.ports.out.InteractionsReadPort;
import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;

import java.util.List;

/** Empty interaction state for profiles used by tests that do not need a real Cassandra instance. */
public class InMemoryInteractionsReadAdapter implements InteractionsReadPort {

    @Override
    public List<ContinueWatchingItem> findContinueWatching(String profileId) {
        return List.of();
    }

    @Override
    public List<String> findRecommendations(String profileId) {
        return List.of();
    }
}
