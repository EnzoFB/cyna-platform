package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a user confirms their email address via the verification link,
 * transitioning the account from {@code PENDING_VERIFICATION} to {@code ACTIVE}.
 * The notification module reacts to this by sending the welcome email — which is
 * now deferred from registration to email confirmation.
 */
public record UserEmailVerified(
        UUID userId,
        String email,
        String firstName,
        String lang,
        Instant occurredAt
) implements DomainEvent {}
