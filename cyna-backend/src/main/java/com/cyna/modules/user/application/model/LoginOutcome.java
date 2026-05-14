package com.cyna.modules.user.application.model;

import java.util.UUID;

/**
 * Outcome of a /auth/login call. Two shapes:
 *
 * <ul>
 *   <li>{@link Challenge} — credentials are valid but the user hasn't
 *       trusted this browser yet (no cookie, expired cookie, or first
 *       login). Frontend must show the OTP step.</li>
 *   <li>{@link Authenticated} — credentials valid AND the browser carries
 *       a still-valid trust cookie. OTP is skipped and the user is
 *       authenticated immediately, exactly like the post-OTP path.</li>
 * </ul>
 */
public sealed interface LoginOutcome permits LoginOutcome.Challenge, LoginOutcome.Authenticated {

    record Challenge(UUID challengeId, long expiresInSeconds) implements LoginOutcome {}

    record Authenticated(AuthTokens tokens) implements LoginOutcome {}
}
