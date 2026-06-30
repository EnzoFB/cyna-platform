package com.cyna.modules.user.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a user changes their password; consumers send a security alert. */
public record UserPasswordChangedIntegrationEvent(
        UUID userId,
        String email,
        String firstName,
        String lang,
        Instant occurredAt
) implements IntegrationEvent {}
