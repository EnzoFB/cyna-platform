package com.cyna.modules.user.application.model;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
