package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;

/**
 * Raised when a login two-step challenge is created and the one-time code must
 * be delivered. Published inside the login transaction, so the notification
 * module sends it AFTER_COMMIT — the code never reaches the user if the
 * challenge failed to persist.
 *
 * <p>The code travels in the event because delivery is decoupled into the
 * notification module; this is an in-process event (same trust boundary as the
 * former direct method call), not something that crosses the network.
 */
public record LoginOtpRequested(
        String email,
        String otpCode,
        Instant expiresAt,
        String lang,
        Instant occurredAt
) implements DomainEvent {}
