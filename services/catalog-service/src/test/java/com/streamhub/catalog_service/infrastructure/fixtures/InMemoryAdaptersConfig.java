package com.streamhub.catalog_service.infrastructure.fixtures;

import com.streamhub.catalog_service.application.ports.out.HomeViewCachePort;
import com.streamhub.catalog_service.application.ports.out.InteractionsReadPort;
import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Test fixtures: the "memory" profile replaces Redis and MongoDB with sample data, so the web tests need no containers. */
@Configuration
@Profile("memory")
public class InMemoryAdaptersConfig {

    @Bean
    public HomeViewCachePort homeViewCachePort() {
        return new InMemoryHomeViewAdapter();
    }

    @Bean
    public TitleSummaryPort titleSummaryPort() {
        return new InMemoryTitleSummaryAdapter();
    }

    @Bean
    public InteractionsReadPort interactionsReadPort() {
        return new InMemoryInteractionsReadAdapter();
    }
}
