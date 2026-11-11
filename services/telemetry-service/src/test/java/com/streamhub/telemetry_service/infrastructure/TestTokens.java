package com.streamhub.telemetry_service.infrastructure;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Builds tokens like auth-service does, with a secret that only exists in the tests. */
public final class TestTokens {

    public static final String SECRET = "test-only-secret-never-used-in-production-0123456789";
    public static final String ISSUER = "streamhub-auth";

    private TestTokens() {
    }

    public static String profileToken(String profileId) {
        return sign(SECRET, ISSUER, profileId, Instant.now().plusSeconds(600));
    }

    public static String accountToken() {
        return sign(SECRET, ISSUER, null, Instant.now().plusSeconds(600));
    }

    public static String sign(String secret, String issuer, String profileId, Instant expiration) {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .issuer(issuer)
                .expirationTime(Date.from(expiration));
        if (profileId != null) {
            claims.claim("profile_id", profileId);
        }
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
