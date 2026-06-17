package com.cyna.modules.user.application.model;

/**
 * Outcome of self-service registration. The account is created in
 * {@code PENDING_VERIFICATION} and no tokens are issued — the user must confirm
 * their email before they can authenticate.
 */
public record RegistrationResult(
        String status,
        String email
) {}
