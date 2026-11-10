package com.streamhub.recommendation_worker.infrastructure.adapters.out.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.streamhub.recommendation_worker.application.ports.out.EventStorePort;
import com.streamhub.recommendation_worker.domain.model.HistoryEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Repository
public class CassandraEventAdapter implements EventStorePort {

    private static final String INSERT_EVENT = """
            INSERT INTO events_by_id (event_id, profile_id, content_id, event_type, payload, received_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_HISTORY = """
            INSERT INTO event_history_by_profile
                (profile_id, event_timestamp, event_id, event_type, content_id, episode_id,
                 progress_seconds, rating)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) USING TIMESTAMP ? AND TTL ?
            """;
    private static final String SELECT_HISTORY = """
            SELECT profile_id, event_timestamp, event_id, event_type, content_id, episode_id,
                   progress_seconds, rating
            FROM event_history_by_profile WHERE profile_id = ? LIMIT ?
            """;
    private final CqlSession session;
    private final PreparedStatement insertEvent;
    private final PreparedStatement insertHistory;
    private final PreparedStatement selectHistory;
    private final int historyTtlSeconds;

    public CassandraEventAdapter(CqlSession session,
                                 @Value("${cassandra.history-ttl-days:90}") long historyTtlDays) {
        this.session = session;
        this.insertEvent = session.prepare(INSERT_EVENT);
        this.insertHistory = session.prepare(INSERT_HISTORY);
        this.selectHistory = session.prepare(SELECT_HISTORY);
        this.historyTtlSeconds = Math.toIntExact(Duration.ofDays(historyTtlDays).toSeconds());
    }

    @Override
    public void recordEvent(String eventId, String profileId, String contentId, String eventType,
                            String payload, Instant receivedAt) {
        session.execute(insertEvent.bind(
                CassandraSupport.eventUuid(eventId), profileId, contentId, eventType, payload, receivedAt));
    }

    @Override
    public void recordHistory(HistoryEntry entry) {
        session.execute(insertHistory.bind(
                entry.profileId(),
                entry.eventTimestamp(),
                CassandraSupport.eventUuid(entry.eventId()),
                entry.eventType(),
                entry.contentId(),
                entry.episodeId(),
                entry.progressSeconds(),
                entry.rating(),
                CassandraSupport.micros(entry.eventTimestamp()),
                historyTtlSeconds));
    }

    @Override
    public List<HistoryEntry> findHistory(String profileId, int limit) {
        return session.execute(selectHistory.bind(profileId, limit)).all().stream()
                .map(CassandraEventAdapter::toEntry)
                .toList();
    }

    private static HistoryEntry toEntry(Row row) {
        Integer progressSeconds = row.isNull("progress_seconds") ? null : row.getInt("progress_seconds");
        return new HistoryEntry(
                row.getString("profile_id"),
                row.getInstant("event_timestamp"),
                row.getUuid("event_id").toString(),
                row.getString("event_type"),
                row.getString("content_id"),
                row.getString("episode_id"),
                progressSeconds,
                row.getString("rating"));
    }
}
