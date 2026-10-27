package com.streamhub.auth_service.application.ports.out;

import java.util.UUID;

/**
 * Issues the two token types of spec.md 1.3: the account token (claim sub) and the
 * profile token (claims sub and profile_id).
 */
public interface TokenIssuerPort {

    IssuedToken issueAccountToken(UUID accountId);

    IssuedToken issueProfileToken(UUID accountId, String profileId);

    record IssuedToken(String value, long expiresIn) {
    }
}
