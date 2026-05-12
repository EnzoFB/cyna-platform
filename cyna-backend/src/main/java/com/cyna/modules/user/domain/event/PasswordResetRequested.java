package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a user (or anyone with their email) successfully requested a
 * password reset. The listener mails the reset link to the registered address.
 *
 * <p>The {@code rawToken} field is the value that must appear in the email —
 * never stored elsewhere. Carrying it on the event keeps the persistence layer
 * and the dispatch layer ignorant of each other.</p>
 */
public record PasswordResetRequested(
        UUID userId,
        String email,
        String firstName,
        String rawToken,
        String lang,
        Instant occurredAt
) implements DomainEvent {}
