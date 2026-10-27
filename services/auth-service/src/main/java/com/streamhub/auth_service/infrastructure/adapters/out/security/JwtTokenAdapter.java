package com.streamhub.auth_service.infrastructure.adapters.out.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.streamhub.auth_service.application.ports.out.TokenIssuerPort;
import com.streamhub.auth_service.infrastructure.config.JwtProperties;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Issues and verifies HS256 tokens. Verification is also used by the filter that protects the endpoints. */
public class JwtTokenAdapter implements TokenIssuerPort {

    public static final String PROFILE_CLAIM = "profile_id";

    private final byte[] secret;
    private final String issuer;
    private final long expirationSeconds;
    private final Clock clock;

    public JwtTokenAdapter(JwtProperties properties, Clock clock) {
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        this.issuer = properties.issuer();
        this.expirationSeconds = properties.expirationSeconds();
        this.clock = clock;
        if (secret.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes long");
        }
    }

    @Override
    public IssuedToken issueAccountToken(UUID accountId) {
        return issue(accountId, null);
    }

    @Override
    public IssuedToken issueProfileToken(UUID accountId, String profileId) {
        return issue(accountId, profileId);
    }

    /** Returns the claims when the signature, the issuer and the expiration are valid, otherwise empty. */
    public Optional<TokenClaims> verify(String token) {
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
            return Optional.of(new TokenClaims(UUID.fromString(claims.getSubject()), claims.getStringClaim(PROFILE_CLAIM)));
        } catch (ParseException | JOSEException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private IssuedToken issue(UUID accountId, String profileId) {
        Instant now = clock.instant();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(accountId.toString())
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(expirationSeconds)));
        if (profileId != null) {
            claims.claim(PROFILE_CLAIM, profileId);
        }
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
            jwt.sign(new MACSigner(secret));
            return new IssuedToken(jwt.serialize(), expirationSeconds);
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign the token", e);
        }
    }

    public record TokenClaims(UUID accountId, String profileId) {
    }
}
