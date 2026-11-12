package com.streamhub.recommendation_worker.application.usecases;

import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenreAffinityRecommendationStrategyTest {

    private static final Instant AT = Instant.parse("2026-10-06T20:00:00Z");
    private static final Map<String, List<String>> CATALOG = Map.of(
            "mov_liked", List.of("Drama", "Thriller"),
            "mov_disliked", List.of("Comedy"),
            "mov_drama", List.of("Drama"),
            "mov_drama_thriller", List.of("Drama", "Thriller"),
            "mov_comedy", List.of("Comedy"),
            "mov_drama_comedy", List.of("Drama", "Comedy"),
            "mov_no_genres", List.of());

    private final GenreAffinityRecommendationStrategy strategy = new GenreAffinityRecommendationStrategy();

    @Test
    void suggestsNewTitlesOfTheLikedGenresBestMatchFirst() {
        List<String> result = strategy.recommend("prof_1", Map.of("mov_liked", 1.0), List.of(), CATALOG);

        assertEquals(List.of("mov_drama_thriller", "mov_drama", "mov_drama_comedy"), result);
    }

    @Test
    void aDislikedGenreLowersTheScoreOfItsTitles() {
        List<String> result = strategy.recommend("prof_1",
                Map.of("mov_liked", 1.0, "mov_disliked", -1.0), List.of(), CATALOG);

        assertEquals(List.of("mov_drama_thriller", "mov_drama"), result);
    }

    @Test
    void watchedTitlesAddAffinityAndAreNeverSuggested() {
        List<ProgressSnapshot> progress = List.of(new ProgressSnapshot("prof_1", "mov_drama", null, 300, AT));

        List<String> result = strategy.recommend("prof_1", Map.of(), progress, CATALOG);

        assertEquals(List.of("mov_drama_comedy", "mov_drama_thriller", "mov_liked"), result);
    }

    @Test
    void ratedTitlesAreNeverSuggested() {
        List<String> result = strategy.recommend("prof_1", Map.of("mov_drama", 1.0), List.of(), CATALOG);

        assertTrue(!result.contains("mov_drama"));
    }

    @Test
    void withoutPositiveSignalsThereAreNoSuggestions() {
        assertTrue(strategy.recommend("prof_1", Map.of(), List.of(), CATALOG).isEmpty());
        assertTrue(strategy.recommend("prof_1", Map.of("mov_disliked", -1.0), List.of(), CATALOG).isEmpty());
    }

    @Test
    void titlesWithoutGenresAndUnknownTitlesAreIgnored() {
        List<String> result = strategy.recommend("prof_1", Map.of("mov_unknown", 1.0), List.of(), CATALOG);

        assertTrue(result.isEmpty());
    }

    @Test
    void atMostTwentyTitlesAreSuggested() {
        Map<String, List<String>> catalog = new HashMap<>();
        catalog.put("mov_liked", List.of("Drama"));
        for (int i = 0; i < 30; i++) {
            catalog.put("mov_" + i, List.of("Drama"));
        }

        assertEquals(GenreAffinityRecommendationStrategy.MAX_RECOMMENDATIONS,
                strategy.recommend("prof_1", Map.of("mov_liked", 1.0), List.of(), catalog).size());
    }
}
