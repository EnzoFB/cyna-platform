package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a user account is anonymized under an RGPD Art. 17 erasure
 * request (the account had a legally-retained transactional footprint, so it
 * was scrubbed-and-kept rather than hard-deleted). Carries no personal data —
 * only the surrogate id and the instant — so it is safe to log/audit.
 */
public record UserAnonymized(
        UUID userId,
        Instant occurredAt
) implements DomainEvent {}
