package com.cyna.modules.user.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a user account is created in an active state (admin-created or post-verification). */
public record UserRegisteredIntegrationEvent(
        UUID userId,
        String email,
        String firstName,
        String role,
        String lang,
        Instant occurredAt
) implements IntegrationEvent {}
