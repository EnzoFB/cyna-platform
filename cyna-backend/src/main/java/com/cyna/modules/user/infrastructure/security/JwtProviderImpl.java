package com.cyna.modules.user.infrastructure.security;

import com.cyna.modules.user.application.port.JwtProvider;
import com.cyna.modules.user.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtProviderImpl implements JwtProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;

    public JwtProviderImpl(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.expiration-ms}") long accessTokenExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    @Override
    public String generateAccessToken(User user) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().value().toString())
                .claim("email", user.getEmail().value())
                .claim("roles", List.of(user.getRole().name()))
                .issuer("cyna-platform")
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Map<String, Object> validateAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Map.of(
                "userId", claims.getSubject(),
                "email", claims.get("email", String.class),
                "roles", claims.get("roles", List.class)
        );
    }

    @Override
    public UUID extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    @Override
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
