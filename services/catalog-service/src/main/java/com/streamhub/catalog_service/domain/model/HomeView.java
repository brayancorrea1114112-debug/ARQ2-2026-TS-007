package com.streamhub.catalog_service.domain.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The precomputed Home of a profile, as it is stored in Redis (spec.md 2.4). It only holds title
 * ids: the names and posters are added later to build a {@link HomeScreen}.
 */
public record HomeView(String profileId, Instant generatedAt, List<CategoryRef> categories,
                       List<ContinueWatchingItem> continueWatching, List<String> recommended) {

    public HomeView {
        Require.notBlank(profileId, "profileId");
        Require.notNull(generatedAt, "generatedAt");
        categories = Require.listOrEmpty(categories);
        continueWatching = Require.listOrEmpty(continueWatching);
        recommended = Require.listOrEmpty(recommended);
    }

    /**
     * The ids of the titles whose name and poster the screen shows (categories and recommended),
     * without repeats and in order of appearance. It is the list that must be looked up in the
     * catalog. The continue-watching entries are not included: the screen answers them as they are
     * (spec.md 3.4).
     */
    public List<String> displayedTitleIds() {
        Set<String> ids = new LinkedHashSet<>();
        categories.forEach(category -> ids.addAll(category.titleIds()));
        ids.addAll(recommended);
        return List.copyOf(ids);
    }
}
