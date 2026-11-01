package com.streamhub.recommendation_worker.domain.model;

import java.time.Instant;

public record RatingSnapshot(
        String profileId,
        String contentId,
        String rating,
        Instant updatedAt
) {
}
