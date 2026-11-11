package com.streamhub.catalog_service.infrastructure.config;

import com.streamhub.catalog_service.application.ports.out.HomeViewCachePort;
import com.streamhub.catalog_service.application.ports.out.InteractionsReadPort;
import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.application.usecases.HomeService;
import com.streamhub.catalog_service.infrastructure.security.JwtTokenVerifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.Duration;

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
    public HomeService homeService(HomeViewCachePort homeViews, TitleSummaryPort titles,
                                   @Lazy InteractionsReadPort interactions, Clock clock,
                                   @Value("${catalog.home-cache-ttl:15m}") Duration homeTtl,
                                   @Value("${catalog.continue-watching-cache-ttl:24h}") Duration continueWatchingTtl) {
        return new HomeService(homeViews, titles, interactions, clock, homeTtl, continueWatchingTtl);
    }
}
