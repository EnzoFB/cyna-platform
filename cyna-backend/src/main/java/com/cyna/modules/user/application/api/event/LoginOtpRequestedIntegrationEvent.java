package com.cyna.modules.user.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;

/** Published when a login OTP must be delivered. */
public record LoginOtpRequestedIntegrationEvent(
        String email,
        String otpCode,
        Instant expiresAt,
        String lang,
        Instant occurredAt
) implements IntegrationEvent {}
