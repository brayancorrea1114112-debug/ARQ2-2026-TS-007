package com.streamhub.catalog_service.infrastructure.config;

import com.streamhub.catalog_service.application.ports.out.HomeViewCachePort;
import com.streamhub.catalog_service.application.ports.out.TitleSummaryPort;
import com.streamhub.catalog_service.infrastructure.adapters.out.mongo.MongoTitleSummaryAdapter;
import com.streamhub.catalog_service.infrastructure.adapters.out.mongo.TrendingCachingTitleSummaryAdapter;
import com.streamhub.catalog_service.infrastructure.adapters.out.redis.RedisHomeViewAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.observability.ContextProviderFactory;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** The Redis and MongoDB adapters. The tests use the "memory" profile to replace them with in-memory fixtures. */
@Configuration
@Profile("!memory")
public class RealAdaptersConfig {

    @Bean
    public HomeViewCachePort homeViewCachePort(StringRedisTemplate redis,
                                               @Value("${catalog.cache-ttl-jitter}") Duration ttlJitter) {
        return new RedisHomeViewAdapter(redis, ttlJitter);
    }

    @Bean
    public TitleSummaryPort titleSummaryPort(MongoTemplate mongo, Clock clock,
                                             @Value("${catalog.trending-cache-ttl}") Duration trendingTtl) {
        return new TrendingCachingTitleSummaryAdapter(new MongoTitleSummaryAdapter(mongo), clock, trendingTtl);
    }

    /** MongoDB waits 30 s to find a server by default; the Home cannot wait that long to answer 503. */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoTimeouts(@Value("${catalog.mongodb.timeout}") Duration timeout,
                                                              ObservationRegistry observations) {
        int millis = (int) timeout.toMillis();
        return settings -> settings
                // Each MongoDB query becomes a span of the trace (Feature 5).
                .contextProvider(ContextProviderFactory.create(observations))
                .addCommandListener(new MongoObservationCommandListener(observations))
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(millis, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(socket -> socket
                        .connectTimeout(millis, TimeUnit.MILLISECONDS)
                        .readTimeout(millis, TimeUnit.MILLISECONDS));
    }
}
