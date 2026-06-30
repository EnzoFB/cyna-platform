package com.cyna.modules.user.domain.model;

import com.cyna.shared.domain.Guard;

import java.time.Instant;
import java.util.UUID;

/**
 * A browser/device the user has previously authenticated from. The presence
 * of a matching {@code token_hash} on a /auth/login call lets the backend
 * skip the OTP step and emit access + refresh tokens directly — same UX
 * every modern SaaS ships (Stripe, GitHub, Notion).
 *
 * <p>Storage: the raw device token is set on the browser as an HttpOnly
 * cookie; only its SHA-256 hash lives here. Same pattern as the refresh
 * token / password reset token / OTP challenge.</p>
 *
 * <p>Expiry is "sliding": every successful trust hit calls
 * {@link #renew(java.time.Duration)} which both updates {@code lastUsedAt}
 * and pushes {@code expiresAt} forward. An active user stays trusted; a
 * dormant one (>30 d no login) gets re-challenged.</p>
 */
public record TrustedDevice(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt,
        Instant lastUsedAt,
        String userAgent
) {

    public static TrustedDevice issue(UUID userId, String tokenHash, Instant expiresAt, String userAgent) {
        Guard.againstNull(userId, "userId");
        Guard.againstNullOrBlank(tokenHash, "tokenHash");
        Guard.againstNull(expiresAt, "expiresAt");

        Instant now = Instant.now();
        return new TrustedDevice(
                UUID.randomUUID(),
                userId,
                tokenHash,
                expiresAt,
                now,
                now,
                userAgent
        );
    }

    public static TrustedDevice reconstitute(
            UUID id,
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt,
            Instant lastUsedAt,
            String userAgent
    ) {
        Guard.againstNull(id, "id");
        Guard.againstNull(userId, "userId");
        Guard.againstNullOrBlank(tokenHash, "tokenHash");
        Guard.againstNull(expiresAt, "expiresAt");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(lastUsedAt, "lastUsedAt");

        return new TrustedDevice(id, userId, tokenHash, expiresAt, createdAt, lastUsedAt, userAgent);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Slides the expiry forward and bumps lastUsedAt. Returns a fresh
     * record so the caller persists it.
     */
    public TrustedDevice renew(Instant newExpiresAt) {
        Guard.againstNull(newExpiresAt, "newExpiresAt");
        return new TrustedDevice(
                id, userId, tokenHash, newExpiresAt, createdAt, Instant.now(), userAgent
        );
    }
}
