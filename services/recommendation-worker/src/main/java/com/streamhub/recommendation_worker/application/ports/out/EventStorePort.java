package com.streamhub.recommendation_worker.application.ports.out;

import com.streamhub.recommendation_worker.domain.model.HistoryEntry;

import java.time.Instant;
import java.util.List;

public interface EventStorePort {

    void recordEvent(String eventId, String profileId, String contentId, String eventType,
                     String payload, Instant receivedAt);

    void recordHistory(HistoryEntry entry);

    void registerProfileActivity(String profileId, Instant at);

    List<HistoryEntry> findHistory(String profileId, int limit);

    List<String> findProfileIds();
}
