package com.streamhub.auth_service.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Master account (spec.md 2.1). The password is only ever kept as a hash. */
public record Account(UUID id, String email, String passwordHash, Instant createdAt) {
}
