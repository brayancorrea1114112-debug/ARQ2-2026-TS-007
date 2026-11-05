package com.streamhub.telemetry_service.infrastructure.adapters.out.rabbit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.streamhub.telemetry_service.application.ports.out.EventPublisherPort;
import com.streamhub.telemetry_service.domain.exception.ServiceUnavailableException;
import com.streamhub.telemetry_service.domain.model.EventContext;
import com.streamhub.telemetry_service.domain.model.PingCommand;
import com.streamhub.telemetry_service.domain.model.RatingCommand;
import com.streamhub.telemetry_service.infrastructure.config.RabbitMqConfig;
import com.streamhub.telemetry_service.infrastructure.web.CorrelationIdFilter;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes with publisher confirms. If the broker does not acknowledge the event, the
 * service answers 503 and the client retries with the same event_id (spec.md 3.5 and 3.6).
 */
public class RabbitEventPublisherAdapter implements EventPublisherPort {

    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;

    public RabbitEventPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishPing(String profileId, PingCommand command, EventContext context) {
        PingEvent event = new PingEvent(command.eventId(), profileId, command.contentId(),
                command.episodeId(), command.progressSeconds(), command.timestamp());
        publish(RabbitMqConfig.TELEMETRY_EXCHANGE, RabbitMqConfig.PING_ROUTING_KEY, event,
                command.eventId(), context);
    }

    @Override
    public void publishRating(String profileId, RatingCommand command, EventContext context) {
        RatingEvent event = new RatingEvent(command.eventId(), profileId, command.contentId(),
                command.rating().name(), command.timestamp());
        publish(RabbitMqConfig.INTERACTIONS_EXCHANGE, RabbitMqConfig.RATING_ROUTING_KEY, event,
                command.eventId(), context);
    }

    private void publish(String exchange, String routingKey, Object event, String eventId,
                         EventContext context) {
        CorrelationData correlationData = new CorrelationData(eventId);
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
                MessageProperties properties = message.getMessageProperties();
                properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                properties.setMessageId(eventId);
                if (hasText(context.correlationId())) {
                    properties.setHeader(CorrelationIdFilter.HEADER, context.correlationId());
                }
                // Fallback for the W3C header; the RabbitTemplate observation also injects it.
                if (hasText(context.traceparent())) {
                    properties.setHeader("traceparent", context.traceparent());
                }
                return message;
            }, correlationData);

            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlationData.getReturned() != null) {
                throw new ServiceUnavailableException("The message broker");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("The message broker", e);
        } catch (ExecutionException | TimeoutException | AmqpException e) {
            throw new ServiceUnavailableException("The message broker", e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PingEvent(
            @JsonProperty("event_id") String eventId,
            @JsonProperty("profile_id") String profileId,
            @JsonProperty("content_id") String contentId,
            @JsonProperty("episode_id") String episodeId,
            @JsonProperty("progress_seconds") int progressSeconds,
            @JsonProperty("timestamp") Instant timestamp
    ) {
    }

    record RatingEvent(
            @JsonProperty("event_id") String eventId,
            @JsonProperty("profile_id") String profileId,
            @JsonProperty("content_id") String contentId,
            @JsonProperty("rating") String rating,
            @JsonProperty("timestamp") Instant timestamp
    ) {
    }
}
