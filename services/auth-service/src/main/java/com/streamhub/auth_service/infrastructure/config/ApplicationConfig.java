package com.streamhub.auth_service.infrastructure.config;

import com.streamhub.auth_service.application.ports.out.AccountRepositoryPort;
import com.streamhub.auth_service.application.ports.out.PasswordHasherPort;
import com.streamhub.auth_service.application.ports.out.ProfileRepositoryPort;
import com.streamhub.auth_service.application.usecases.AuthService;
import com.streamhub.auth_service.application.usecases.ProfileService;
import com.streamhub.auth_service.infrastructure.adapters.out.security.JwtTokenAdapter;
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
    public JwtTokenAdapter jwtTokenAdapter(JwtProperties properties, Clock clock) {
        return new JwtTokenAdapter(properties, clock);
    }

    @Bean
    public AuthService authService(AccountRepositoryPort accounts, PasswordHasherPort passwordHasher,
                                   JwtTokenAdapter tokenIssuer) {
        return new AuthService(accounts, passwordHasher, tokenIssuer);
    }

    @Bean
    public ProfileService profileService(ProfileRepositoryPort profiles, JwtTokenAdapter tokenIssuer) {
        return new ProfileService(profiles, tokenIssuer);
    }
}
