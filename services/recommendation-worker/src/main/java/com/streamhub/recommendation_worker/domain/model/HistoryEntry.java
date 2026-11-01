package com.streamhub.recommendation_worker.domain.model;

import java.time.Instant;

public record HistoryEntry(
        String profileId,
        Instant eventTimestamp,
        String eventId,
        String eventType,
        String contentId,
        String episodeId,
        Integer progressSeconds,
        String rating
) {
}
