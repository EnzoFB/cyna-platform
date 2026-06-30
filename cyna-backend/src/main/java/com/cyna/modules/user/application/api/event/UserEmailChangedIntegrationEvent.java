package com.cyna.modules.user.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a user changes their account email; the alert goes to the previous address. */
public record UserEmailChangedIntegrationEvent(
        UUID userId,
        String oldEmail,
        String newEmail,
        String firstName,
        String lang,
        Instant occurredAt
) implements IntegrationEvent {}
