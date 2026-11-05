package com.streamhub.catalog_service.domain.model;

import java.util.List;

/** A season of a series. It always has at least one episode (spec.md 2.3). */
public record Season(int number, List<Episode> episodes) {

    public Season {
        Require.positive(number, "season number");
        episodes = Require.nonEmptyList(episodes, "season episodes");
    }
}
