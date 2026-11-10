package com.streamhub.telemetry_service.infrastructure.config;

import com.streamhub.telemetry_service.application.ports.out.EventPublisherPort;
import com.streamhub.telemetry_service.application.usecases.TelemetryIngestionService;
import com.streamhub.telemetry_service.infrastructure.security.JwtTokenVerifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** The use cases are plain classes: this is where they are wired to their adapters. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public JwtTokenVerifier jwtTokenVerifier(JwtProperties properties, Clock clock) {
        return new JwtTokenVerifier(properties, clock);
    }

    @Bean
    public TelemetryIngestionService telemetryIngestionService(EventPublisherPort publisher) {
        return new TelemetryIngestionService(publisher);
    }
}
