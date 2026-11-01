package com.streamhub.recommendation_worker.infrastructure.adapters.in.rabbit;

import com.streamhub.recommendation_worker.application.ports.in.RecommendationWorkerUseCase;
import com.streamhub.recommendation_worker.domain.model.PingReceivedEvent;
import com.streamhub.recommendation_worker.domain.model.RatingAddedEvent;
import com.streamhub.recommendation_worker.infrastructure.config.RabbitMqConfig;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Acknowledgment is managed by the container. Invalid payloads fail fast and go to the dead-letter
 * queue; infrastructure failures are retried (1s, 5s, 25s) before being rejected to the DLQ.
 */
@Component
public class RecommendationMessageListener {

    private final RecommendationWorkerUseCase useCase;

    public RecommendationMessageListener(RecommendationWorkerUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = RabbitMqConfig.PING_QUEUE)
    public void onPingReceived(PingReceivedEvent event) {
        try {
            useCase.handlePing(event);
        } catch (IllegalArgumentException invalidPayload) {
            throw new AmqpRejectAndDontRequeueException(invalidPayload);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.RATING_QUEUE)
    public void onRatingAdded(RatingAddedEvent event) {
        try {
            useCase.handleRating(event);
        } catch (IllegalArgumentException invalidPayload) {
            throw new AmqpRejectAndDontRequeueException(invalidPayload);
        }
    }
}
