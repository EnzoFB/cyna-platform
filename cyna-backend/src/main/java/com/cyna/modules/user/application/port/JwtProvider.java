package com.cyna.modules.user.application.port;

import com.cyna.modules.user.domain.model.User;

import java.util.Map;
import java.util.UUID;

public interface JwtProvider {

    String generateAccessToken(User user);

    String generateRefreshToken();

    Map<String, Object> validateAccessToken(String token);

    UUID extractUserId(String token);

    long getAccessTokenExpirationHours();

    long getRefreshTokenExpirationHours();
}
