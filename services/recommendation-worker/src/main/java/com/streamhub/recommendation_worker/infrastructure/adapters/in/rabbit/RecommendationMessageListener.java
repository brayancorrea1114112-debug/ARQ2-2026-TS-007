package com.streamhub.recommendation_worker.infrastructure.adapters.in.rabbit;

import com.streamhub.recommendation_worker.application.ports.in.RecommendationWorkerUseCase;
import com.streamhub.recommendation_worker.domain.model.PingReceivedEvent;
import com.streamhub.recommendation_worker.domain.model.RatingAddedEvent;
import com.streamhub.recommendation_worker.infrastructure.config.RabbitMqConfig;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Acknowledgment is managed by the container. Invalid payloads fail fast and go to the dead-letter
 * queue; infrastructure failures get 3 attempts (waits of 1s and 5s) before being rejected to the DLQ.
 * The X-Correlation-ID of the event goes into the logs of its processing (spec.md 1.3).
 */
@Component
public class RecommendationMessageListener {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    private final RecommendationWorkerUseCase useCase;

    public RecommendationMessageListener(RecommendationWorkerUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = RabbitMqConfig.PING_QUEUE)
    public void onPingReceived(PingReceivedEvent event,
                               @Header(name = CORRELATION_HEADER, required = false) String correlationId) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
            useCase.handlePing(event);
        } catch (IllegalArgumentException invalidPayload) {
            throw new AmqpRejectAndDontRequeueException(invalidPayload);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.RATING_QUEUE)
    public void onRatingAdded(RatingAddedEvent event,
                              @Header(name = CORRELATION_HEADER, required = false) String correlationId) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
            useCase.handleRating(event);
        } catch (IllegalArgumentException invalidPayload) {
            throw new AmqpRejectAndDontRequeueException(invalidPayload);
        }
    }
}
