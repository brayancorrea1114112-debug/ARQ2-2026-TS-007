package com.streamhub.recommendation_worker.infrastructure.adapters.out.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.streamhub.recommendation_worker.application.ports.out.RatingStorePort;
import com.streamhub.recommendation_worker.domain.model.RatingSnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CassandraRatingAdapter implements RatingStorePort {

    private static final String INSERT = """
            INSERT INTO ratings_by_profile (profile_id, content_id, rating, updated_at)
            VALUES (?, ?, ?, ?) USING TIMESTAMP ?
            """;
    private static final String SELECT_BY_PROFILE_AND_CONTENT = """
            SELECT profile_id, content_id, rating, updated_at
            FROM ratings_by_profile WHERE profile_id = ? AND content_id = ?
            """;
    private static final String SELECT_BY_PROFILE = """
            SELECT profile_id, content_id, rating, updated_at
            FROM ratings_by_profile WHERE profile_id = ?
            """;

    private final CqlSession session;
    private final PreparedStatement insert;
    private final PreparedStatement selectOne;
    private final PreparedStatement selectByProfile;

    public CassandraRatingAdapter(CqlSession session) {
        this.session = session;
        this.insert = session.prepare(INSERT);
        this.selectOne = session.prepare(SELECT_BY_PROFILE_AND_CONTENT);
        this.selectByProfile = session.prepare(SELECT_BY_PROFILE);
    }

    @Override
    public void saveRating(RatingSnapshot snapshot) {
        session.execute(insert.bind(
                snapshot.profileId(),
                snapshot.contentId(),
                snapshot.rating(),
                snapshot.updatedAt(),
                CassandraSupport.micros(snapshot.updatedAt())));
    }

    @Override
    public Optional<RatingSnapshot> find(String profileId, String contentId) {
        return session.execute(selectOne.bind(profileId, contentId)).all().stream()
                .findFirst()
                .map(CassandraRatingAdapter::toSnapshot);
    }

    @Override
    public List<RatingSnapshot> findByProfile(String profileId) {
        return session.execute(selectByProfile.bind(profileId)).all().stream()
                .map(CassandraRatingAdapter::toSnapshot)
                .toList();
    }

    private static RatingSnapshot toSnapshot(Row row) {
        return new RatingSnapshot(
                row.getString("profile_id"),
                row.getString("content_id"),
                row.getString("rating"),
                row.getInstant("updated_at"));
    }
}
