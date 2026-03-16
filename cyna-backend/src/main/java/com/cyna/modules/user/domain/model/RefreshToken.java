package com.cyna.modules.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        UUID id,
        UserId userId,
        String tokenHash,
        Instant expiresAt,
        boolean revoked,
        Instant createdAt
) {
    public static RefreshToken create(UserId userId, String tokenHash, Instant expiresAt) {
        return new RefreshToken(
                UUID.randomUUID(),
                userId,
                tokenHash,
                expiresAt,
                false,
                Instant.now()
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
