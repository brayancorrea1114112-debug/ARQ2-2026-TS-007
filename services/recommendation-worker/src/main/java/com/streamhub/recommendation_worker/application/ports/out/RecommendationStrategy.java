package com.streamhub.recommendation_worker.application.ports.out;

import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;
import com.streamhub.recommendation_worker.domain.model.RatingSnapshot;

import java.util.List;

/**
 * Seam for the suggestion engine. The catalog/genre based strategy can replace the default one
 * without touching the worker (hexagonal architecture, ADR 004).
 */
public interface RecommendationStrategy {

    List<String> recommend(String profileId, List<RatingSnapshot> ratings, List<ProgressSnapshot> progress);
}
