package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a user requests an email-address change and a confirmation token
 * has been minted. The notification module sends the confirmation link to the
 * new address. Published inside the request transaction → delivered
 * AFTER_COMMIT, so no link is sent for a token that was rolled back.
 */
public record EmailChangeRequested(
        UUID userId,
        String newEmail,
        String firstName,
        String token,
        String lang,
        Instant occurredAt
) implements DomainEvent {}
