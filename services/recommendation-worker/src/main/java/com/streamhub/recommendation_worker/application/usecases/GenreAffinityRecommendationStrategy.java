package com.streamhub.recommendation_worker.application.usecases;

import com.streamhub.recommendation_worker.application.ports.out.RecommendationStrategy;
import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Suggestion engine by genre affinity (ADR 009). Each rated title moves its genres by its preference weight
 * (LIKE +1, DISLIKE -1) and each started title adds a smaller weight, because watching is a weaker signal than
 * rating. The suggestions are the titles the profile has neither rated nor started, ordered by the sum of the
 * weights of their genres; only titles with a positive score are suggested.
 */
@Component
public class GenreAffinityRecommendationStrategy implements RecommendationStrategy {

    static final int MAX_RECOMMENDATIONS = 20;
    static final double WATCHED_WEIGHT = 0.5;

    @Override
    public List<String> recommend(String profileId, Map<String, Double> preferences,
                                  List<ProgressSnapshot> progress, Map<String, List<String>> genresByTitle) {
        Map<String, Double> genreWeights = new HashMap<>();
        Set<String> seen = new HashSet<>(preferences.keySet());
        preferences.forEach((contentId, weight) -> addToGenres(genreWeights, genresByTitle, contentId, weight));
        for (ProgressSnapshot snapshot : progress) {
            seen.add(snapshot.contentId());
            addToGenres(genreWeights, genresByTitle, snapshot.contentId(), WATCHED_WEIGHT);
        }

        Map<String, Double> scores = new HashMap<>();
        genresByTitle.forEach((contentId, genres) -> {
            if (!seen.contains(contentId)) {
                scores.put(contentId, genres.stream().mapToDouble(genre -> genreWeights.getOrDefault(genre, 0.0)).sum());
            }
        });

        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_RECOMMENDATIONS)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static void addToGenres(Map<String, Double> genreWeights, Map<String, List<String>> genresByTitle,
                                    String contentId, double weight) {
        for (String genre : genresByTitle.getOrDefault(contentId, List.of())) {
            genreWeights.merge(genre, weight, Double::sum);
        }
    }
}
