package com.streamhub.recommendation_worker.application.usecases;

import com.streamhub.recommendation_worker.application.ports.out.EventStorePort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Rebuilds the disposable Redis read model from the Cassandra source of truth (ADR 006).
 */
@Service
public class RedisProjectionRebuilder {

    private final HomeProjection projection;
    private final EventStorePort eventStore;

    public RedisProjectionRebuilder(HomeProjection projection, EventStorePort eventStore) {
        this.projection = projection;
        this.eventStore = eventStore;
    }

    public void rebuildProfile(String profileId) {
        projection.projectFull(profileId);
    }

    public int rebuildAll() {
        List<String> profileIds = eventStore.findProfileIds();
        profileIds.forEach(projection::projectFull);
        return profileIds.size();
    }
}
