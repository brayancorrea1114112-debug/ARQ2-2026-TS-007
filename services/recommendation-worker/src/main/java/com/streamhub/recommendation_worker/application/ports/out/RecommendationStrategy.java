package com.streamhub.recommendation_worker.application.ports.out;

import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;

import java.util.List;
import java.util.Map;

/**
 * Seam for the suggestion engine: another algorithm can replace the current one without touching the
 * worker (hexagonal architecture, ADR 004).
 */
public interface RecommendationStrategy {

    /**
     * @param preferences   content_id to its preference weight (LIKE +1, DISLIKE -1) of the profile
     * @param progress      titles the profile has started
     * @param genresByTitle content_id to its genres, for every title of the catalog
     * @return content_ids to suggest, best first
     */
    List<String> recommend(String profileId, Map<String, Double> preferences, List<ProgressSnapshot> progress,
                           Map<String, List<String>> genresByTitle);
}
