package com.streamhub.catalog_service.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.streamhub.catalog_service.infrastructure.config.JwtProperties;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Verifies the HS256 tokens issued by auth-service. This service never issues tokens. */
public class JwtTokenVerifier {

    public static final String PROFILE_CLAIM = "profile_id";

    private final byte[] secret;
    private final String issuer;
    private final Clock clock;

    public JwtTokenVerifier(JwtProperties properties, Clock clock) {
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        this.issuer = properties.issuer();
        this.clock = clock;
        if (secret.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes long");
        }
    }

    /** Returns the identity when the signature, the issuer and the expiration are valid, otherwise empty. */
    public Optional<AuthenticatedUser> verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (!issuer.equals(claims.getIssuer()) || expiration == null || !expiration.toInstant().isAfter(clock.instant())) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(UUID.fromString(claims.getSubject()), claims.getStringClaim(PROFILE_CLAIM)));
        } catch (ParseException | JOSEException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
