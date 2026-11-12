package com.streamhub.catalog_service.domain.model;

import java.util.List;

/** The genres of a title (spec.md 2.3). The Recommendation Worker uses them to suggest similar titles. */
public record TitleGenres(String contentId, List<String> genres) {

    public TitleGenres {
        Require.notBlank(contentId, "contentId");
        genres = genres == null ? List.of() : List.copyOf(genres);
    }
}
