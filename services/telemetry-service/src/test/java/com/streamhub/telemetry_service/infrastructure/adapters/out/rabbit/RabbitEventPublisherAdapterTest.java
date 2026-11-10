package com.streamhub.telemetry_service.infrastructure.adapters.out.rabbit;

import com.streamhub.telemetry_service.domain.exception.ServiceUnavailableException;
import com.streamhub.telemetry_service.domain.model.EventContext;
import com.streamhub.telemetry_service.domain.model.PingCommand;
import com.streamhub.telemetry_service.domain.model.Rating;
import com.streamhub.telemetry_service.domain.model.RatingCommand;
import com.streamhub.telemetry_service.infrastructure.config.RabbitMqConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitEventPublisherAdapterTest {

    private static final String EVENT_ID = "0f6c2a52-6a53-4f7e-8a56-9d8c6a6b5f10";
    private static final String CORRELATION_ID = "5b9e1c1e-6f0a-4d6b-9f55-6c0f1f3a7d11";
    private static final String TRACEPARENT =
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

    private RabbitTemplate rabbitTemplate;
    private RabbitEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        adapter = new RabbitEventPublisherAdapter(rabbitTemplate);
    }

    @Test
    void publishesThePingWithTheWorkerContract() {
        PingCommand command = new PingCommand(EVENT_ID, "mov_8832", "ep_102", 1420,
                Instant.parse("2026-10-06T20:00:00Z"));
        EventContext context = new EventContext(CORRELATION_ID, TRACEPARENT);
        confirmPublication();

        adapter.publishPing("prof_9921", command, context);

        Captured captured = capture();
        assertThat(captured.exchange()).isEqualTo(RabbitMqConfig.TELEMETRY_EXCHANGE);
        assertThat(captured.routingKey()).isEqualTo(RabbitMqConfig.PING_ROUTING_KEY);
        assertThat(captured.event()).isInstanceOf(RabbitEventPublisherAdapter.PingEvent.class);

        RabbitEventPublisherAdapter.PingEvent event =
                (RabbitEventPublisherAdapter.PingEvent) captured.event();
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.profileId()).isEqualTo("prof_9921");
        assertThat(event.contentId()).isEqualTo("mov_8832");
        assertThat(event.episodeId()).isEqualTo("ep_102");
        assertThat(event.progressSeconds()).isEqualTo(1420);
        assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-10-06T20:00:00Z"));
        assertThat(captured.correlationData().getId()).isEqualTo(EVENT_ID);

        Message message = applyProcessor(captured.processor());
        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(EVENT_ID);
        assertThat((Object) message.getMessageProperties().getHeader("X-Correlation-ID"))
                .isEqualTo(CORRELATION_ID);
        assertThat((Object) message.getMessageProperties().getHeader("traceparent"))
                .isEqualTo(TRACEPARENT);
    }

    @Test
    void publishesTheRatingWithTheWorkerContract() {
        RatingCommand command = new RatingCommand(EVENT_ID, "mov_8832", Rating.LIKE,
                Instant.parse("2026-10-06T20:01:15Z"));
        confirmPublication();

        adapter.publishRating("prof_9921", command, new EventContext(CORRELATION_ID, TRACEPARENT));

        Captured captured = capture();
        assertThat(captured.exchange()).isEqualTo(RabbitMqConfig.INTERACTIONS_EXCHANGE);
        assertThat(captured.routingKey()).isEqualTo(RabbitMqConfig.RATING_ROUTING_KEY);
        assertThat(captured.event()).isInstanceOf(RabbitEventPublisherAdapter.RatingEvent.class);

        RabbitEventPublisherAdapter.RatingEvent event =
                (RabbitEventPublisherAdapter.RatingEvent) captured.event();
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.profileId()).isEqualTo("prof_9921");
        assertThat(event.contentId()).isEqualTo("mov_8832");
        assertThat(event.rating()).isEqualTo("LIKE");
        assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-10-06T20:01:15Z"));
    }

    @Test
    void aNackIsAnsweredAs503() {
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);
            correlationData.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatThrownBy(() -> adapter.publishPing("prof_9921",
                new PingCommand(EVENT_ID, "mov_8832", null, 10, Instant.now()),
                new EventContext(CORRELATION_ID, null)))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void aBrokerErrorIsAnsweredAs503() {
        doThrow(new AmqpException("broker down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatThrownBy(() -> adapter.publishRating("prof_9921",
                new RatingCommand(EVENT_ID, "mov_8832", Rating.LIKE, Instant.now()),
                new EventContext(CORRELATION_ID, null)))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    private void confirmPublication() {
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    private Captured capture() {
        ArgumentCaptor<String> exchange = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CorrelationData> correlationData = ArgumentCaptor.forClass(CorrelationData.class);

        verify(rabbitTemplate).convertAndSend(exchange.capture(), routingKey.capture(), event.capture(),
                processor.capture(), correlationData.capture());

        return new Captured(exchange.getValue(), routingKey.getValue(), event.getValue(),
                processor.getValue(), correlationData.getValue());
    }

    private static Message applyProcessor(MessagePostProcessor processor) {
        Message message = new Message(new byte[0], new MessageProperties());
        return processor.postProcessMessage(message);
    }

    private record Captured(String exchange, String routingKey, Object event,
                            MessagePostProcessor processor, CorrelationData correlationData) {
    }
}
