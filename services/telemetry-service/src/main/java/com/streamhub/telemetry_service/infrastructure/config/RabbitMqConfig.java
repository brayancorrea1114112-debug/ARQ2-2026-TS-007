package com.streamhub.telemetry_service.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The topology is identical to the one declared by the recommendation worker. Declaring it
 * here too means the messages wait in the queue even if the worker starts later.
 */
@Configuration
@Profile("!memory")
public class RabbitMqConfig {

    public static final String TELEMETRY_EXCHANGE = "streamhub.telemetry";
    public static final String INTERACTIONS_EXCHANGE = "streamhub.interactions";

    public static final String PING_ROUTING_KEY = "telemetry.ping.received";
    public static final String RATING_ROUTING_KEY = "interaction.rating.added";

    public static final String PING_QUEUE = "telemetry.progress.queue";
    public static final String PING_DLQ = "telemetry.progress.dlq";
    public static final String RATING_QUEUE = "interactions.rating.queue";
    public static final String RATING_DLQ = "interactions.rating.dlq";

    /** Events use the snake_case contract defined in spec.md (event_id, profile_id, ...). */
    @Bean
    MessageConverter jsonMessageConverter() {
        JsonMapper eventMapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return new JacksonJsonMessageConverter(eventMapper);
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
                .with(PING_ROUTING_KEY);
    }

    @Bean
    Binding ratingBinding() {
        return BindingBuilder.bind(interactionRatingQueue())
                .to(interactionsExchange())
                .with(RATING_ROUTING_KEY);
    }
}
