package com.streamhub.recommendation_worker.infrastructure.adapters.out.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.streamhub.recommendation_worker.application.ports.out.RecommendationStorePort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class CassandraRecommendationAdapter implements RecommendationStorePort {

    private static final String UPSERT = """
            INSERT INTO recommendations_by_profile (profile_id, content_ids, generated_at)
            VALUES (?, ?, ?) USING TIMESTAMP ?
            """;

    private final PreparedStatement upsert;
    private final CqlSession session;

    public CassandraRecommendationAdapter(CqlSession session) {
        this.session = session;
        this.upsert = session.prepare(UPSERT);
    }

    @Override
    public void saveRecommendations(String profileId, List<String> contentIds, Instant generatedAt) {
        session.execute(upsert.bind(profileId, contentIds, generatedAt,
                CassandraSupport.micros(generatedAt)));
    }
}
