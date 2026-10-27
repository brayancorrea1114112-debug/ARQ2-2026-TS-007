package com.streamhub.auth_service.infrastructure.security;

import java.util.UUID;

/** Identity taken from a valid token. profileId is null for an account token. */
public record AuthenticatedUser(UUID accountId, String profileId) {
}
