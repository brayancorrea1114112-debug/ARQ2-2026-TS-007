package com.streamhub.recommendation_worker.infrastructure.adapters.out.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.streamhub.recommendation_worker.application.ports.out.PreferenceStorePort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class CassandraPreferenceAdapter implements PreferenceStorePort {

    private static final String INSERT = """
            INSERT INTO prefs_by_profile (profile_id, content_id, weight, updated_at)
            VALUES (?, ?, ?, ?) USING TIMESTAMP ?
            """;
    private static final String SELECT_BY_PROFILE = """
            SELECT content_id, weight FROM prefs_by_profile WHERE profile_id = ?
            """;

    private final CqlSession session;
    private final PreparedStatement insert;
    private final PreparedStatement selectByProfile;

    public CassandraPreferenceAdapter(CqlSession session) {
        this.session = session;
        this.insert = session.prepare(INSERT);
        this.selectByProfile = session.prepare(SELECT_BY_PROFILE);
    }

    @Override
    public void savePreference(String profileId, String contentId, double weight, Instant updatedAt) {
        session.execute(insert.bind(profileId, contentId, weight, updatedAt,
                CassandraSupport.micros(updatedAt)));
    }

    @Override
    public Map<String, Double> findByProfile(String profileId) {
        Map<String, Double> preferences = new LinkedHashMap<>();
        session.execute(selectByProfile.bind(profileId)).forEach(row ->
                preferences.put(row.getString("content_id"), row.getDouble("weight")));
        return preferences;
    }
}
