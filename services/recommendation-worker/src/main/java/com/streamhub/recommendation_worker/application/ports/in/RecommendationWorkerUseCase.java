package com.streamhub.recommendation_worker.application.ports.in;

import com.streamhub.recommendation_worker.domain.model.PingReceivedEvent;
import com.streamhub.recommendation_worker.domain.model.RatingAddedEvent;

public interface RecommendationWorkerUseCase {
    void handlePing(PingReceivedEvent event);
    void handleRating(RatingAddedEvent event);
}
