package com.streamhub.recommendation_worker.application.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamhub.recommendation_worker.application.ports.out.CatalogGenresPort;
import com.streamhub.recommendation_worker.application.ports.out.EventStorePort;
import com.streamhub.recommendation_worker.application.ports.out.PreferenceStorePort;
import com.streamhub.recommendation_worker.application.ports.out.ProgressStorePort;
import com.streamhub.recommendation_worker.application.ports.out.RecommendationStorePort;
import com.streamhub.recommendation_worker.application.ports.out.RecommendationStrategy;
import com.streamhub.recommendation_worker.application.ports.out.RatingStorePort;
import com.streamhub.recommendation_worker.domain.model.PingReceivedEvent;
import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;
import com.streamhub.recommendation_worker.domain.model.RatingAddedEvent;
import com.streamhub.recommendation_worker.domain.model.RatingSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

class DefaultRecommendationWorkerUseCaseTest {

    private static final String EVENT_ID = "0f6c2a52-6a53-4f7e-8a56-9d8c6a6b5f10";
    private static final Instant AT = Instant.parse("2026-10-06T20:00:00Z");

    private ProgressStorePort progressStore;
    private RatingStorePort ratingStore;
    private PreferenceStorePort preferenceStore;
    private EventStorePort eventStore;
    private RecommendationStorePort recommendationStore;
    private RecommendationStrategy recommendationStrategy;
    private CatalogGenresPort catalogGenres;
    private DefaultRecommendationWorkerUseCase useCase;

    @BeforeEach
    void setUp() {
        progressStore = mock(ProgressStorePort.class);
        ratingStore = mock(RatingStorePort.class);
        preferenceStore = mock(PreferenceStorePort.class);
        eventStore = mock(EventStorePort.class);
        recommendationStore = mock(RecommendationStorePort.class);
        recommendationStrategy = mock(RecommendationStrategy.class);
        catalogGenres = mock(CatalogGenresPort.class);
        when(catalogGenres.genresByTitle()).thenReturn(Map.of("mov_77", List.of("Drama")));
        when(recommendationStrategy.recommend(anyString(), anyMap(), anyList(), anyMap())).thenReturn(List.of("mov_77"));
        useCase = new DefaultRecommendationWorkerUseCase(progressStore, ratingStore, preferenceStore,
                eventStore, recommendationStore, recommendationStrategy, catalogGenres,
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void handlePing_persistsInCassandraAndStoresRecommendationsWithoutRedis() {
        PingReceivedEvent event = new PingReceivedEvent(EVENT_ID, "prof_1", "mov_1", "ep_1", 1200, AT);

        useCase.handlePing(event);

        verify(eventStore).recordEvent(eq(EVENT_ID), eq("prof_1"), eq("mov_1"),
                eq("telemetry.ping.received"), any(), any());
        verify(progressStore).saveProgress(new ProgressSnapshot("prof_1", "mov_1", "ep_1", 1200, AT));

        verify(recommendationStore).saveRecommendations("prof_1", List.of("mov_77"), AT);

        InOrder order = inOrder(progressStore, recommendationStore);
        order.verify(progressStore).saveProgress(any());
        order.verify(recommendationStore).saveRecommendations("prof_1", List.of("mov_77"), AT);
    }

    @Test
    void handlePing_rejectsNegativeProgress() {
        PingReceivedEvent event = new PingReceivedEvent(EVENT_ID, "prof_1", "mov_1", null, -1, AT);

        assertThrows(IllegalArgumentException.class, () -> useCase.handlePing(event));

        verify(progressStore, never()).saveProgress(any());
    }

    @Test
    void handleRating_storesRatingPreferenceAndRecommendationsInCassandra() {
        RatingAddedEvent event = new RatingAddedEvent(EVENT_ID, "prof_1", "mov_1", "LIKE", AT);

        useCase.handleRating(event);

        verify(ratingStore).saveRating(new RatingSnapshot("prof_1", "mov_1", "LIKE", AT));
        verify(preferenceStore).savePreference("prof_1", "mov_1", 1.0, AT);

        verify(recommendationStore).saveRecommendations("prof_1", List.of("mov_77"), AT);

        InOrder order = inOrder(preferenceStore, recommendationStore);
        order.verify(preferenceStore).savePreference(eq("prof_1"), eq("mov_1"), anyDouble(), eq(AT));
        order.verify(recommendationStore).saveRecommendations("prof_1", List.of("mov_77"), AT);
    }

    @Test
    void handleRating_usesThePreferencesAndTheCatalogGenres() {
        when(preferenceStore.findByProfile("prof_1")).thenReturn(Map.of("mov_1", 1.0));

        useCase.handleRating(new RatingAddedEvent(EVENT_ID, "prof_1", "mov_1", "LIKE", AT));

        verify(recommendationStrategy).recommend(eq("prof_1"), eq(Map.of("mov_1", 1.0)), anyList(),
                eq(Map.of("mov_77", List.of("Drama"))));
    }

    @Test
    void whenTheCatalogIsDownTheEventIsPersistedAndThePreviousRecommendationsAreKept() {
        when(catalogGenres.genresByTitle()).thenThrow(new CatalogGenresPort.CatalogUnavailableException("down", null));

        useCase.handlePing(new PingReceivedEvent(EVENT_ID, "prof_1", "mov_1", null, 60, AT));

        verify(progressStore).saveProgress(new ProgressSnapshot("prof_1", "mov_1", null, 60, AT));
        verify(recommendationStore, never()).saveRecommendations(anyString(), anyList(), any());
    }

    @Test
    void handleRating_rejectsUnknownRating() {
        RatingAddedEvent event = new RatingAddedEvent(EVENT_ID, "prof_1", "mov_1", "MAYBE", AT);

        assertThrows(IllegalArgumentException.class, () -> useCase.handleRating(event));

        verify(ratingStore, never()).saveRating(any());
    }
}
