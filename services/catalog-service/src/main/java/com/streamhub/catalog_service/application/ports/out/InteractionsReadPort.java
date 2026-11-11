package com.streamhub.catalog_service.application.ports.out;

import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;

import java.util.List;

/** Reads the durable profile state needed to rebuild a Home view after a cache miss. */
public interface InteractionsReadPort {

    List<ContinueWatchingItem> findContinueWatching(String profileId);

    List<String> findRecommendations(String profileId);
}
