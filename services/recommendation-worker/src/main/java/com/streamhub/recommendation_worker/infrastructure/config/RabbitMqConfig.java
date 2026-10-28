package com.streamhub.recommendation_worker.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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
