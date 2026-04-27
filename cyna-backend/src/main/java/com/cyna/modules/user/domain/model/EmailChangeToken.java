package com.cyna.modules.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public record EmailChangeToken(
        UUID id,
        UUID userId,
        String newEmail,
        String token,
        Instant expiresAt,
        Instant createdAt
) {
    public static EmailChangeToken create(UUID userId, String newEmail, String token, Instant expiresAt) {
        return new EmailChangeToken(UUID.randomUUID(), userId, newEmail, token, expiresAt, Instant.now());
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
