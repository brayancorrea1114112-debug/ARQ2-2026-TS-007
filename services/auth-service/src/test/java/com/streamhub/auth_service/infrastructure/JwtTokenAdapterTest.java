package com.streamhub.auth_service.infrastructure;

import com.streamhub.auth_service.application.ports.out.TokenIssuerPort.IssuedToken;
import com.streamhub.auth_service.infrastructure.adapters.out.security.JwtTokenAdapter;
import com.streamhub.auth_service.infrastructure.adapters.out.security.JwtTokenAdapter.TokenClaims;
import com.streamhub.auth_service.infrastructure.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenAdapterTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes!!";
    private static final Instant NOW = Instant.parse("2026-10-06T20:00:00Z");

    private final UUID accountId = UUID.randomUUID();

    private JwtTokenAdapter adapterAt(Instant instant, String secret, String issuer) {
        return new JwtTokenAdapter(new JwtProperties(secret, issuer, 3600), Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void accountTokenCarriesOnlyTheAccountId() {
        JwtTokenAdapter adapter = adapterAt(NOW, SECRET, "streamhub-auth");

        IssuedToken token = adapter.issueAccountToken(accountId);
        TokenClaims claims = adapter.verify(token.value()).orElseThrow();

        assertEquals(3600, token.expiresIn());
        assertEquals(accountId, claims.accountId());
        assertNull(claims.profileId());
    }

    @Test
    void profileTokenCarriesTheProfileId() {
        JwtTokenAdapter adapter = adapterAt(NOW, SECRET, "streamhub-auth");

        TokenClaims claims = adapter.verify(adapter.issueProfileToken(accountId, "prof_9921").value()).orElseThrow();

        assertEquals(accountId, claims.accountId());
        assertEquals("prof_9921", claims.profileId());
    }

    @Test
    void expiredTokenIsRejected() {
        String token = adapterAt(NOW, SECRET, "streamhub-auth").issueAccountToken(accountId).value();
        JwtTokenAdapter later = adapterAt(NOW.plus(Duration.ofHours(2)), SECRET, "streamhub-auth");

        assertEquals(Optional.empty(), later.verify(token));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = adapterAt(NOW, "another-secret-with-at-least-32-bytes", "streamhub-auth")
                .issueAccountToken(accountId).value();

        assertEquals(Optional.empty(), adapterAt(NOW, SECRET, "streamhub-auth").verify(token));
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        String token = adapterAt(NOW, SECRET, "someone-else").issueAccountToken(accountId).value();

        assertEquals(Optional.empty(), adapterAt(NOW, SECRET, "streamhub-auth").verify(token));
    }

    @Test
    void garbageIsRejectedWithoutThrowing() {
        JwtTokenAdapter adapter = adapterAt(NOW, SECRET, "streamhub-auth");

        assertTrue(adapter.verify("not-a-jwt").isEmpty());
        assertTrue(adapter.verify("").isEmpty());
    }

    @Test
    void aShortSecretIsRefusedAtStartup() {
        assertThrows(IllegalStateException.class, () -> adapterAt(NOW, "short", "streamhub-auth"));
    }
}
