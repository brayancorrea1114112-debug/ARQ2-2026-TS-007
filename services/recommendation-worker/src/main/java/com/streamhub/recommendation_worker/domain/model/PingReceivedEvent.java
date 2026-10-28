package com.streamhub.recommendation_worker.domain.model;

import java.time.Instant;

public record PingReceivedEvent(
        String eventId,
        String profileId,
        String contentId,
        String episodeId,
        Integer progressSeconds,
        Instant timestamp
) {
}
