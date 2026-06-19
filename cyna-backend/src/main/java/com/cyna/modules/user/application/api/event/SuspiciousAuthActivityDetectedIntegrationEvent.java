package com.cyna.modules.user.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when refresh-token reuse is detected. {@code reason} is flattened to
 * a String (not the domain enum) so the contract stays transport-friendly across
 * a future service boundary.
 */
public record SuspiciousAuthActivityDetectedIntegrationEvent(
        UUID userId,
        String email,
        String firstName,
        String lang,
        String reason,
        Instant occurredAt
) implements IntegrationEvent {}
