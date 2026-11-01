package com.streamhub.recommendation_worker.infrastructure.adapters.out.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.streamhub.recommendation_worker.application.ports.out.ProgressStorePort;
import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CassandraProgressAdapter implements ProgressStorePort {

    private static final String INSERT = """
            INSERT INTO progress_by_profile
                (profile_id, content_id, episode_id, progress_seconds, last_event_timestamp)
            VALUES (?, ?, ?, ?, ?) USING TIMESTAMP ?
            """;
    private static final String SELECT_BY_PROFILE_AND_CONTENT = """
            SELECT profile_id, content_id, episode_id, progress_seconds, last_event_timestamp
            FROM progress_by_profile WHERE profile_id = ? AND content_id = ?
            """;
    private static final String SELECT_BY_PROFILE = """
            SELECT profile_id, content_id, episode_id, progress_seconds, last_event_timestamp
            FROM progress_by_profile WHERE profile_id = ?
            """;

    private final CqlSession session;
    private final PreparedStatement insert;
    private final PreparedStatement selectOne;
    private final PreparedStatement selectByProfile;

    public CassandraProgressAdapter(CqlSession session) {
        this.session = session;
        this.insert = session.prepare(INSERT);
        this.selectOne = session.prepare(SELECT_BY_PROFILE_AND_CONTENT);
        this.selectByProfile = session.prepare(SELECT_BY_PROFILE);
    }

    @Override
    public void saveProgress(ProgressSnapshot snapshot) {
        session.execute(insert.bind(
                snapshot.profileId(),
                snapshot.contentId(),
                snapshot.episodeId(),
                snapshot.progressSeconds(),
                snapshot.lastEventTimestamp(),
                CassandraSupport.micros(snapshot.lastEventTimestamp())));
    }

    @Override
    public Optional<ProgressSnapshot> find(String profileId, String contentId) {
        return session.execute(selectOne.bind(profileId, contentId)).all().stream()
                .findFirst()
                .map(CassandraProgressAdapter::toSnapshot);
    }

    @Override
    public List<ProgressSnapshot> findByProfile(String profileId) {
        return session.execute(selectByProfile.bind(profileId)).all().stream()
                .map(CassandraProgressAdapter::toSnapshot)
                .toList();
    }

    private static ProgressSnapshot toSnapshot(Row row) {
        return new ProgressSnapshot(
                row.getString("profile_id"),
                row.getString("content_id"),
                row.getString("episode_id"),
                row.getInt("progress_seconds"),
                row.getInstant("last_event_timestamp"));
    }
}
