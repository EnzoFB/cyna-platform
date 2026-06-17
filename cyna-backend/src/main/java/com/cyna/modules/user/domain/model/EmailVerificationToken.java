package com.cyna.modules.user.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One-shot credential issued at registration (and on resend) to confirm the
 * user's email address. Valid 24h.
 *
 * <p>Stored as a SHA-256 hash; the raw value only ever appears in the email
 * sent to the user. The consumed flag enforces single use even if the same link
 * is clicked twice.</p>
 */
public record EmailVerificationToken(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant expiresAt,
        boolean consumed,
        Instant createdAt
) {
    public static EmailVerificationToken create(UUID userId, String tokenHash, Instant expiresAt) {
        return new EmailVerificationToken(
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

    public EmailVerificationToken consume() {
        return new EmailVerificationToken(id, userId, tokenHash, expiresAt, true, createdAt);
    }
}
