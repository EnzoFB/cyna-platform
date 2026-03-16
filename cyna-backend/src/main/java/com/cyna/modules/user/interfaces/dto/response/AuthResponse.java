package com.cyna.modules.user.interfaces.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {
    public static AuthResponse from(String accessToken, String refreshToken, long expiresInMs) {
        return new AuthResponse(accessToken, refreshToken, expiresInMs / 1000, "Bearer");
    }
}
