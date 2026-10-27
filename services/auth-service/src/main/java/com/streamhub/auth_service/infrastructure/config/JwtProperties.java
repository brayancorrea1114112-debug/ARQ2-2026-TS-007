package com.streamhub.auth_service.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The secret comes from the JWT_SECRET environment variable and must be at least 32 bytes (HS256).
 * The issuer is the value Kong uses to find the credential that validates the signature.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, String issuer, long expirationSeconds) {
}
