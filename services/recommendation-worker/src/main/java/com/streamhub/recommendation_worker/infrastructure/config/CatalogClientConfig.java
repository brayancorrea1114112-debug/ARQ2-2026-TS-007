package com.streamhub.recommendation_worker.infrastructure.config;

import com.streamhub.recommendation_worker.application.ports.out.CatalogGenresPort;
import com.streamhub.recommendation_worker.infrastructure.adapters.out.catalog.CachingCatalogGenresAdapter;
import com.streamhub.recommendation_worker.infrastructure.adapters.out.catalog.HttpCatalogGenresClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

/** The client of the Catalog & Home Service: short timeouts, so a slow catalog never blocks the queue (ADR 009). */
@Configuration
public class CatalogClientConfig {

    @Bean
    CatalogGenresPort catalogGenresPort(RestClient.Builder builder,
                                        @Value("${catalog.base-url}") String baseUrl,
                                        @Value("${catalog.timeout}") Duration timeout,
                                        @Value("${catalog.genres-cache-ttl}") Duration cacheTtl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        RestClient restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        return new CachingCatalogGenresAdapter(new HttpCatalogGenresClient(restClient), Clock.systemUTC(), cacheTtl);
    }
}
