package com.streamhub.recommendation_worker.application.usecases;

import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;
import com.streamhub.recommendation_worker.domain.model.RatingSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentAffinityRecommendationStrategyTest {

    private static final Instant AT = Instant.parse("2026-10-06T20:00:00Z");
    private final ContentAffinityRecommendationStrategy strategy = new ContentAffinityRecommendationStrategy();

    @Test
    void recommendsLikedTitlesThatWereNotWatched() {
        List<RatingSnapshot> ratings = List.of(
                new RatingSnapshot("prof_1", "mov_1", "LIKE", AT),
                new RatingSnapshot("prof_1", "mov_2", "DISLIKE", AT),
                new RatingSnapshot("prof_1", "mov_3", "LIKE", AT));
        List<ProgressSnapshot> progress = List.of(
                new ProgressSnapshot("prof_1", "mov_3", null, 100, AT));

        assertEquals(List.of("mov_1"), strategy.recommend("prof_1", ratings, progress));
    }

    @Test
    void returnsEmptyWhenThereAreNoLikes() {
        List<RatingSnapshot> ratings = List.of(new RatingSnapshot("prof_1", "mov_1", "DISLIKE", AT));

        assertTrue(strategy.recommend("prof_1", ratings, List.of()).isEmpty());
    }
}
