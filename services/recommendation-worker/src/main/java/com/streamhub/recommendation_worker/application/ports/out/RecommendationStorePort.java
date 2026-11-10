package com.streamhub.recommendation_worker.application.ports.out;

import java.time.Instant;
import java.util.List;

/** Durable, queryable recommendations for the Catalog & Home Service cache miss path. */
public interface RecommendationStorePort {

    void saveRecommendations(String profileId, List<String> contentIds, Instant generatedAt);
}
