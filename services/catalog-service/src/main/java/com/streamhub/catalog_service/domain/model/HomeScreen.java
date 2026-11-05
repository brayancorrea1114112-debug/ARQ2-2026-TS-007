package com.streamhub.catalog_service.domain.model;

import java.time.Instant;
import java.util.List;

/** The Home screen answered to the client by GET /api/v1/home (spec.md 3.4). */
public record HomeScreen(String profileId, Instant generatedAt, HomeSource source, List<HomeCategory> categories,
                         List<ContinueWatchingItem> continueWatching, List<TitleSummary> recommended) {

    public HomeScreen {
        Require.notBlank(profileId, "profileId");
        Require.notNull(generatedAt, "generatedAt");
        Require.notNull(source, "source");
        categories = Require.listOrEmpty(categories);
        continueWatching = Require.listOrEmpty(continueWatching);
        recommended = Require.listOrEmpty(recommended);
    }
}
