package com.streamhub.auth_service.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Family subprofile of a master account (spec.md 2.2). */
public record Profile(String id, UUID accountId, String name, ProfileType type, String avatarUrl, Instant createdAt) {

    public boolean belongsTo(UUID otherAccountId) {
        return accountId.equals(otherAccountId);
    }
}
