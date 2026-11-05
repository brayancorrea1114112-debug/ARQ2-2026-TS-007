package com.streamhub.catalog_service.domain.model;

/** One episode of a season (spec.md 2.3). */
public record Episode(String episodeId, String title, int durationSeconds) {

    public Episode {
        Require.notBlank(episodeId, "episodeId");
        Require.notBlank(title, "episode title");
        Require.positive(durationSeconds, "episode duration");
    }
}
