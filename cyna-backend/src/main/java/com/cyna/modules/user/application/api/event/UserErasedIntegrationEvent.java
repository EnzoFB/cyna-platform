package com.cyna.modules.user.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a user account is hard-deleted (RGPD Art. 17, no retained
 * footprint). Carries no personal data — only the surrogate id. Every module
 * holding data keyed by this user reacts by purging it; this is the
 * split-ready replacement for the cross-schema {@code ON DELETE CASCADE} that
 * does the cleanup while the modules share one database.
 */
public record UserErasedIntegrationEvent(
        UUID userId,
        Instant occurredAt
) implements IntegrationEvent {}
