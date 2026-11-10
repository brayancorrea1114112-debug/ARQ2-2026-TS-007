package com.streamhub.catalog_service.infrastructure.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import java.net.InetSocketAddress;

@Configuration
@Profile("!memory")
public class CassandraConfig {

    @Bean(destroyMethod = "close")
    @Lazy
    public CqlSession cqlSession(
            @Value("${spring.cassandra.contact-points}") String host,
            @Value("${spring.cassandra.port}") int port,
            @Value("${spring.cassandra.local-datacenter}") String datacenter,
            @Value("${spring.cassandra.keyspace-name}") String keyspace) {
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host, port))
                .withLocalDatacenter(datacenter)
                .withKeyspace(keyspace)
                .build();
    }
}
