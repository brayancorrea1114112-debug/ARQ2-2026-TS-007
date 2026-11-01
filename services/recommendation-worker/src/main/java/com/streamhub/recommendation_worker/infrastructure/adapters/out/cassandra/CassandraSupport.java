package com.streamhub.recommendation_worker.infrastructure.adapters.out.cassandra;

import java.time.Instant;
import java.util.UUID;

final class CassandraSupport {

    private CassandraSupport() {
    }

    /**
     * Cassandra write timestamps are microseconds. Using the event time makes redelivered or
     * out-of-order events last-write-wins without reading before writing.
     */
    static long micros(Instant instant) {
        return instant.getEpochSecond() * 1_000_000L + instant.getNano() / 1_000L;
    }

    static UUID eventUuid(String eventId) {
        try {
            return UUID.fromString(eventId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("event_id must be a valid UUID: " + eventId, exception);
        }
    }
}
