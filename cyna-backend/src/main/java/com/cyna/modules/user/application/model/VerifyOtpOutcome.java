package com.cyna.modules.user.application.model;

/**
 * Result of a successful OTP verification: the standard JWT + refresh
 * token pair, plus the freshly-minted raw device token the controller
 * will set as a {@code device_token} cookie so future /auth/login calls
 * on this browser skip the OTP step.
 */
public record VerifyOtpOutcome(AuthTokens tokens, String trustedDeviceToken) {}
