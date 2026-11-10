package com.streamhub.telemetry_service.infrastructure.fixtures;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Test fixtures: the "memory" profile replaces RabbitMQ with a recorder, so the web tests need no broker. */
@Configuration
@Profile("memory")
public class InMemoryAdaptersConfig {

    @Bean
    public InMemoryEventPublisherAdapter eventPublisherPort() {
        return new InMemoryEventPublisherAdapter();
    }
}
