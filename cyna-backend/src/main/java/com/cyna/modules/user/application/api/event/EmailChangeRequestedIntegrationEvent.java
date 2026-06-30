package com.cyna.modules.user.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when an email-change confirmation link must be sent to the new address. */
public record EmailChangeRequestedIntegrationEvent(
        UUID userId,
        String newEmail,
        String firstName,
        String token,
        String lang,
        Instant occurredAt
) implements IntegrationEvent {}
