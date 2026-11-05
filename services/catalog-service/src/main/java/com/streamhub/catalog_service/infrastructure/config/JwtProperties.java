package com.streamhub.catalog_service.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The secret comes from the JWT_SECRET environment variable and must be at least 32 bytes (HS256). */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, String issuer) {
}
