package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a verification email must be sent: at self-service registration
 * and on a resend request. The listener mails the unique confirmation link to
 * the registered address.
 *
 * <p>The {@code rawToken} field is the value that must appear in the email —
 * never stored elsewhere (only its SHA-256 hash is persisted). Carrying it on
 * the event keeps the persistence layer and the dispatch layer ignorant of each
 * other.</p>
 */
public record EmailVerificationRequested(
        UUID userId,
        String email,
        String firstName,
        String rawToken,
        String lang,
        Instant occurredAt
) implements DomainEvent {}
