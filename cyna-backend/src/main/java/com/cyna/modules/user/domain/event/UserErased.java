package com.cyna.modules.user.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a user account is <b>hard-deleted</b> under an RGPD Art. 17
 * erasure (the account had no legally-retained transactional footprint, so
 * nothing is kept). Carries no personal data — only the surrogate id and the
 * instant — so it is safe to propagate and log.
 *
 * <p>Distinct from {@link UserAnonymized}, which is raised on the
 * anonymize-and-keep branch. This event marks the row actually leaving the
 * database: today the dependent rows in other modules are removed by the
 * cross-schema {@code ON DELETE CASCADE} chain; once the databases are split
 * that chain is gone, so this event becomes the trigger each owning module
 * reacts to in order to purge the user's data it holds.
 */
public record UserErased(
        UUID userId,
        Instant occurredAt
) implements DomainEvent {}
