package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a user successfully changes their password.
 *
 * <p>Consumers send a security alert to the registered email — the standard
 * detection mechanism for account takeover (the legitimate user notices a
 * change they did not initiate).</p>
 */
public record UserPasswordChanged(
        UUID userId,
        String email,
        String firstName,
        String lang,
        Instant occurredAt
) implements DomainEvent {}
