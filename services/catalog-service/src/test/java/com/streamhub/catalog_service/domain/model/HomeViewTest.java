package com.streamhub.catalog_service.domain.model;

import com.streamhub.catalog_service.domain.exception.InvalidDomainDataException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeViewTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-10-06T19:58:00Z");

    @Test
    void displayedTitleIdsCollectsCategoriesAndRecommendedOnceInOrderOfAppearance() {
        HomeView view = new HomeView("prof_9921", GENERATED_AT,
                List.of(new CategoryRef("Trending", List.of("mov_1", "mov_2")),
                        new CategoryRef("Drama", List.of("mov_2", "mov_3"))),
                List.of(new ContinueWatchingItem("mov_8832", "ep_102", 1420)),
                List.of("mov_77", "mov_1"));

        assertEquals(List.of("mov_1", "mov_2", "mov_3", "mov_77"), view.displayedTitleIds());
    }

    @Test
    void aViewWithoutSectionsIsValidAndDisplaysNothing() {
        HomeView view = new HomeView("prof_9921", GENERATED_AT, null, null, null);

        assertTrue(view.categories().isEmpty());
        assertTrue(view.displayedTitleIds().isEmpty());
    }

    @Test
    void aViewNeedsAProfileAndAGenerationTime() {
        assertThrows(InvalidDomainDataException.class,
                () -> new HomeView(" ", GENERATED_AT, List.of(), List.of(), List.of()));
        assertThrows(InvalidDomainDataException.class,
                () -> new HomeView("prof_9921", null, List.of(), List.of(), List.of()));
    }

    @Test
    void progressCannotBeNegative() {
        assertThrows(InvalidDomainDataException.class, () -> new ContinueWatchingItem("mov_1", null, -1));
    }

    @Test
    void aScreenNeedsItsSource() {
        assertThrows(InvalidDomainDataException.class,
                () -> new HomeScreen("prof_9921", GENERATED_AT, null, List.of(), List.of(), List.of()));
    }
}
