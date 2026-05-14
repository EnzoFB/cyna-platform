package com.cyna.modules.user.interfaces.dto.response;

import com.cyna.modules.user.application.model.LoginOutcome;

import java.util.UUID;

/**
 * Union shape returned by POST /auth/login. Either:
 *
 * <ul>
 *   <li>OTP required → {@code challengeId} + {@code expiresInSeconds} set,
 *       {@code tokens} null. Frontend shows the OTP form.</li>
 *   <li>Trusted device → {@code tokens} set, {@code challengeId} null.
 *       Frontend treats the response like a post-OTP success: store the
 *       access token, navigate, done.</li>
 * </ul>
 */
public record LoginResponse(
        UUID challengeId,
        Long expiresInSeconds,
        AuthResponse tokens
) {
    public static LoginResponse from(LoginOutcome outcome) {
        return switch (outcome) {
            case LoginOutcome.Challenge c -> new LoginResponse(c.challengeId(), c.expiresInSeconds(), null);
            case LoginOutcome.Authenticated a -> new LoginResponse(
                    null,
                    null,
                    AuthResponse.from(a.tokens().accessToken(), a.tokens().refreshToken(), a.tokens().expiresIn())
            );
        };
    }
}
