package com.streamhub.telemetry_service.domain.model;

import java.time.Instant;

/** Like or dislike accepted by POST /api/v1/interactions/like (spec.md 3.6). */
public record RatingCommand(
        String eventId,
        String contentId,
        Rating rating,
        Instant timestamp
) {
}
