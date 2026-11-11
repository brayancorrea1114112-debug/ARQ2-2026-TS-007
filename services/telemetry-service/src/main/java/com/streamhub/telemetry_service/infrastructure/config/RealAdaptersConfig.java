package com.streamhub.telemetry_service.infrastructure.config;

import com.streamhub.telemetry_service.application.ports.out.EventPublisherPort;
import com.streamhub.telemetry_service.infrastructure.adapters.out.rabbit.RabbitEventPublisherAdapter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** The RabbitMQ adapter. The tests use the "memory" profile to replace it with a fake. */
@Configuration
@Profile("!memory")
public class RealAdaptersConfig {

    @Bean
    public EventPublisherPort eventPublisherPort(RabbitTemplate rabbitTemplate) {
        return new RabbitEventPublisherAdapter(rabbitTemplate);
    }
}
