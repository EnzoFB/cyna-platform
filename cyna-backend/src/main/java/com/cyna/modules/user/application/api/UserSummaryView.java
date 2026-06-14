package com.cyna.modules.user.application.api;

import java.util.UUID;

/**
 * Minimal identity projection for other modules that need to display who a
 * user is (e.g. the admin order listing) without reaching into the user
 * module's tables. Sourced live, so it reflects the current account state
 * (including anonymized accounts).
 */
public record UserSummaryView(
        UUID id,
        String email,
        String firstName,
        String lastName
) {}
