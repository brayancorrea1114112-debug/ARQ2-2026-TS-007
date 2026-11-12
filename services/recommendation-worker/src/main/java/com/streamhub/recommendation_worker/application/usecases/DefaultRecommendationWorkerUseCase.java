package com.streamhub.recommendation_worker.application.usecases;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamhub.recommendation_worker.application.ports.in.RecommendationWorkerUseCase;
import com.streamhub.recommendation_worker.application.ports.out.CatalogGenresPort;
import com.streamhub.recommendation_worker.application.ports.out.EventStorePort;
import com.streamhub.recommendation_worker.application.ports.out.PreferenceStorePort;
import com.streamhub.recommendation_worker.application.ports.out.ProgressStorePort;
import com.streamhub.recommendation_worker.application.ports.out.RatingStorePort;
import com.streamhub.recommendation_worker.application.ports.out.RecommendationStorePort;
import com.streamhub.recommendation_worker.application.ports.out.RecommendationStrategy;
import com.streamhub.recommendation_worker.domain.model.HistoryEntry;
import com.streamhub.recommendation_worker.domain.model.PingReceivedEvent;
import com.streamhub.recommendation_worker.domain.model.ProgressSnapshot;
import com.streamhub.recommendation_worker.domain.model.RatingAddedEvent;
import com.streamhub.recommendation_worker.domain.model.RatingSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Persists event state and its recommendation result in Cassandra. Redis is populated on demand
 * by the Catalog & Home Service.
 */
@Service
public class DefaultRecommendationWorkerUseCase implements RecommendationWorkerUseCase {

    static final String PING_EVENT_TYPE = "telemetry.ping.received";
    static final String RATING_EVENT_TYPE = "interaction.rating.added";

    private static final String LIKE = "LIKE";
    private static final String DISLIKE = "DISLIKE";
    private static final Logger log = LoggerFactory.getLogger(DefaultRecommendationWorkerUseCase.class);

    private final ProgressStorePort progressStore;
    private final RatingStorePort ratingStore;
    private final PreferenceStorePort preferenceStore;
    private final EventStorePort eventStore;
    private final RecommendationStorePort recommendationStore;
    private final RecommendationStrategy recommendationStrategy;
    private final CatalogGenresPort catalogGenres;
    private final ObjectMapper objectMapper;

    public DefaultRecommendationWorkerUseCase(ProgressStorePort progressStore,
                                              RatingStorePort ratingStore,
                                              PreferenceStorePort preferenceStore,
                                              EventStorePort eventStore,
                                              RecommendationStorePort recommendationStore,
                                              RecommendationStrategy recommendationStrategy,
                                              CatalogGenresPort catalogGenres,
                                              ObjectMapper objectMapper) {
        this.progressStore = progressStore;
        this.ratingStore = ratingStore;
        this.preferenceStore = preferenceStore;
        this.eventStore = eventStore;
        this.recommendationStore = recommendationStore;
        this.recommendationStrategy = recommendationStrategy;
        this.catalogGenres = catalogGenres;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handlePing(PingReceivedEvent event) {
        validatePing(event);
        Instant receivedAt = Instant.now();

        eventStore.recordEvent(event.eventId(), event.profileId(), event.contentId(),
                PING_EVENT_TYPE, serialize(event), receivedAt);
        eventStore.recordHistory(new HistoryEntry(event.profileId(), event.timestamp(), event.eventId(),
                PING_EVENT_TYPE, event.contentId(), event.episodeId(), event.progressSeconds(), null));
        progressStore.saveProgress(new ProgressSnapshot(event.profileId(), event.contentId(),
                event.episodeId(), event.progressSeconds(), event.timestamp()));

        refreshRecommendations(event.profileId(), event.timestamp());
    }

    @Override
    public void handleRating(RatingAddedEvent event) {
        validateRating(event);
        Instant receivedAt = Instant.now();

        eventStore.recordEvent(event.eventId(), event.profileId(), event.contentId(),
                RATING_EVENT_TYPE, serialize(event), receivedAt);
        eventStore.recordHistory(new HistoryEntry(event.profileId(), event.timestamp(), event.eventId(),
                RATING_EVENT_TYPE, event.contentId(), null, null, event.rating()));
        ratingStore.saveRating(new RatingSnapshot(event.profileId(), event.contentId(),
                event.rating(), event.timestamp()));
        preferenceStore.savePreference(event.profileId(), event.contentId(),
                LIKE.equals(event.rating()) ? 1.0 : -1.0, event.timestamp());

        refreshRecommendations(event.profileId(), event.timestamp());
    }

    /**
     * The event is already persisted at this point. Without genres the previous recommendations are kept instead of
     * failing the message: retrying would not help while the catalog is down (ADR 009).
     */
    private void refreshRecommendations(String profileId, Instant generatedAt) {
        Map<String, List<String>> genres;
        try {
            genres = catalogGenres.genresByTitle();
        } catch (CatalogGenresPort.CatalogUnavailableException e) {
            log.warn("Keeping the previous recommendations of {}: {}", profileId, e.getMessage());
            return;
        }
        recommendationStore.saveRecommendations(profileId,
                recommendationStrategy.recommend(profileId, preferenceStore.findByProfile(profileId),
                        progressStore.findByProfile(profileId), genres),
                generatedAt);
    }

    private static void validatePing(PingReceivedEvent event) {
        if (event == null || event.eventId() == null || event.profileId() == null
                || event.contentId() == null || event.timestamp() == null) {
            throw new IllegalArgumentException("PingReceivedEvent payload is incomplete");
        }
        if (event.progressSeconds() == null || event.progressSeconds() < 0) {
            throw new IllegalArgumentException("progress_seconds must be 0 or greater");
        }
    }

    private static void validateRating(RatingAddedEvent event) {
        if (event == null || event.eventId() == null || event.profileId() == null
                || event.contentId() == null || event.timestamp() == null) {
            throw new IllegalArgumentException("RatingAddedEvent payload is incomplete");
        }
        if (!LIKE.equals(event.rating()) && !DISLIKE.equals(event.rating())) {
            throw new IllegalArgumentException("rating must be LIKE or DISLIKE");
        }
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize the event payload", exception);
        }
    }
}
