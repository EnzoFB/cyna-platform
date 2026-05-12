package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a user successfully changes their account email.
 *
 * <p>Consumers send an alert to the previous email so the original owner is
 * informed — critical detection signal if the account has been hijacked.</p>
 */
public record UserEmailChanged(
        UUID userId,
        String oldEmail,
        String newEmail,
        String firstName,
        String lang,
        Instant occurredAt
) implements DomainEvent {}
