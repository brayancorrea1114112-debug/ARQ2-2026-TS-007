package com.streamhub.recommendation_worker.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String TELEMETRY_EXCHANGE = "streamhub.telemetry";
    public static final String INTERACTIONS_EXCHANGE = "streamhub.interactions";

    public static final String PING_QUEUE = "telemetry.progress.queue";
    public static final String PING_DLQ = "telemetry.progress.dlq";
    public static final String RATING_QUEUE = "interactions.rating.queue";
    public static final String RATING_DLQ = "interactions.rating.dlq";

    /** Events use the snake_case contract defined in spec.md (event_id, profile_id, ...). */
    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        ObjectMapper eventMapper = objectMapper.copy()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(eventMapper);
    }

    @Bean
    TopicExchange telemetryExchange() {
        return new TopicExchange(TELEMETRY_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange interactionsExchange() {
        return new TopicExchange(INTERACTIONS_EXCHANGE, true, false);
    }

    @Bean
    Queue telemetryProgressQueue() {
        return QueueBuilder.durable(PING_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", PING_DLQ)
                .build();
    }

    @Bean
    Queue telemetryProgressDlq() {
        return QueueBuilder.durable(PING_DLQ).build();
    }

    @Bean
    Queue interactionRatingQueue() {
        return QueueBuilder.durable(RATING_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", RATING_DLQ)
                .build();
    }

    @Bean
    Queue interactionRatingDlq() {
        return QueueBuilder.durable(RATING_DLQ).build();
    }

    @Bean
    Binding telemetryBinding() {
        return BindingBuilder.bind(telemetryProgressQueue())
                .to(telemetryExchange())
                .with("telemetry.ping.received");
    }

    @Bean
    Binding ratingBinding() {
        return BindingBuilder.bind(interactionRatingQueue())
                .to(interactionsExchange())
                .with("interaction.rating.added");
    }
}
