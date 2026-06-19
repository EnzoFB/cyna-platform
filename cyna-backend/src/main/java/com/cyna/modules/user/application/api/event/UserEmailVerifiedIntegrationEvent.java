package com.cyna.modules.user.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a user confirms their email and the account becomes active. */
public record UserEmailVerifiedIntegrationEvent(
        UUID userId,
        String email,
        String firstName,
        String lang,
        Instant occurredAt
) implements IntegrationEvent {}
