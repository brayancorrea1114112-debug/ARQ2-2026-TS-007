package com.streamhub.recommendation_worker.application.usecases;

import com.streamhub.recommendation_worker.application.ports.out.RecommendationStrategy;
import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;
import com.streamhub.recommendation_worker.domain.model.RatingSnapshot;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default suggestion engine. It only uses persisted interactions (no catalog access), as the
 * catalog is owned by another module. A genre-based strategy can replace this bean later.
 */
@Component
public class ContentAffinityRecommendationStrategy implements RecommendationStrategy {

    private static final int MAX_RECOMMENDATIONS = 20;

    @Override
    public List<String> recommend(String profileId, List<RatingSnapshot> ratings,
                                  List<ProgressSnapshot> progress) {
        Map<String, Double> score = new HashMap<>();
        for (RatingSnapshot rating : ratings) {
            score.merge(rating.contentId(), weight(rating.rating()), Double::sum);
        }

        Set<String> alreadyWatched = new HashSet<>();
        for (ProgressSnapshot snapshot : progress) {
            alreadyWatched.add(snapshot.contentId());
        }

        return score.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .filter(contentId -> !alreadyWatched.contains(contentId))
                .sorted(Comparator.naturalOrder())
                .limit(MAX_RECOMMENDATIONS)
                .toList();
    }

    private static double weight(String rating) {
        return "LIKE".equals(rating) ? 1.0 : -1.0;
    }
}
