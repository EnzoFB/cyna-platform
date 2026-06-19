package com.cyna.modules.user.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a verification email must be sent. {@code rawToken} is the value to embed in the link. */
public record EmailVerificationRequestedIntegrationEvent(
        UUID userId,
        String email,
        String firstName,
        String rawToken,
        String lang,
        Instant occurredAt
) implements IntegrationEvent {}
