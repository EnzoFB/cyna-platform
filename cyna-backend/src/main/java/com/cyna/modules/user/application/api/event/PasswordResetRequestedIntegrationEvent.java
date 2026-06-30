package com.cyna.modules.user.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a password reset is requested. {@code rawToken} is the value to embed in the reset link. */
public record PasswordResetRequestedIntegrationEvent(
        UUID userId,
        String email,
        String firstName,
        String rawToken,
        String lang,
        Instant occurredAt
) implements IntegrationEvent {}
