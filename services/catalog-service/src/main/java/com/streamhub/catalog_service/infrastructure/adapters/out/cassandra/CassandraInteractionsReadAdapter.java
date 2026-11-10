package com.streamhub.catalog_service.infrastructure.adapters.out.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.Row;
import com.streamhub.catalog_service.application.ports.out.InteractionsReadPort;
import com.streamhub.catalog_service.domain.exception.ServiceUnavailableException;
import com.streamhub.catalog_service.domain.model.ContinueWatchingItem;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("!memory")
public class CassandraInteractionsReadAdapter implements InteractionsReadPort {

    private static final int CONTINUE_WATCHING_LIMIT = 20;

    private static final String SELECT_PROGRESS = """
            SELECT content_id, episode_id, progress_seconds, last_event_timestamp
            FROM progress_by_profile WHERE profile_id = ?
            """;
    private static final String SELECT_RECOMMENDATIONS = """
            SELECT content_ids FROM recommendations_by_profile WHERE profile_id = ?
            """;

    private final CqlSession session;

    public CassandraInteractionsReadAdapter(@Lazy CqlSession session) {
        this.session = session;
    }

    @Override
    public List<ContinueWatchingItem> findContinueWatching(String profileId) {
        try {
            return session.execute(session.prepare(SELECT_PROGRESS).bind(profileId)).all().stream()
                    .sorted((left, right) -> right.getInstant("last_event_timestamp")
                            .compareTo(left.getInstant("last_event_timestamp")))
                    .limit(CONTINUE_WATCHING_LIMIT)
                    .map(CassandraInteractionsReadAdapter::toContinueWatching)
                    .toList();
        } catch (DriverException | BeanCreationException e) {
            throw new ServiceUnavailableException("The Interactions DB", e);
        }
    }

    @Override
    public List<String> findRecommendations(String profileId) {
        try {
            Row row = session.execute(session.prepare(SELECT_RECOMMENDATIONS).bind(profileId)).one();
            return row == null || row.isNull("content_ids")
                    ? List.of()
                    : List.copyOf(row.getList("content_ids", String.class));
        } catch (DriverException | BeanCreationException e) {
            throw new ServiceUnavailableException("The Interactions DB", e);
        }
    }

    private static ContinueWatchingItem toContinueWatching(Row row) {
        return new ContinueWatchingItem(row.getString("content_id"), row.getString("episode_id"),
                row.getInt("progress_seconds"));
    }
}
