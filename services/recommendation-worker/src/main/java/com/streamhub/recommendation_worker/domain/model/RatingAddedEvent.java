package com.streamhub.recommendation_worker.domain.model;

import java.time.Instant;

public record RatingAddedEvent(
        String eventId,
        String profileId,
        String contentId,
        String rating,
        Instant timestamp
) {
}
