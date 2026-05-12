package com.cyna.modules.user.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One-shot credential issued when the user requests a password reset.
 *
 * <p>Stored as a SHA-256 hash; the raw value only ever appears in the email
 * sent to the user. Consumed flag enforces single use even if the same link
 * is clicked twice.</p>
 */
public record PasswordResetToken(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant expiresAt,
        boolean consumed,
        Instant createdAt
) {
    public static PasswordResetToken create(UUID userId, String tokenHash, Instant expiresAt) {
        return new PasswordResetToken(
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

    public PasswordResetToken consume() {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, true, createdAt);
    }
}
