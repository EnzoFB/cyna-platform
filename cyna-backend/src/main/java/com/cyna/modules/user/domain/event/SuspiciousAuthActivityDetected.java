package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when refresh token reuse is detected — a strong signal that a token
 * was leaked and replayed by an attacker. The legitimate session has been
 * scorched-earth revoked; the user must be informed immediately.
 */
public record SuspiciousAuthActivityDetected(
        UUID userId,
        String email,
        String firstName,
        String lang,
        Reason reason,
        Instant occurredAt
) implements DomainEvent {

    public enum Reason {
        REFRESH_TOKEN_REUSE
    }
}
