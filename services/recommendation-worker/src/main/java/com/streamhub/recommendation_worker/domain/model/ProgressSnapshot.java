package com.streamhub.recommendation_worker.domain.model;

import java.time.Instant;

public record ProgressSnapshot(
        String profileId,
        String contentId,
        String episodeId,
        int progressSeconds,
        Instant lastEventTimestamp
) {
}
