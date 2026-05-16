package com.cyna.modules.user.infrastructure.security;

import com.cyna.shared.infrastructure.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Manages the {@code device_token} cookie that lets a returning user skip
 * the OTP step on /auth/login. Attributes mirror the refresh cookie
 * (HttpOnly, Secure, SameSite=Lax, Path=/api/v1/auth) and the lifetime
 * is driven by {@code app.auth.trusted-device.expiration-days}. Default
 * 30 days, the standard SaaS window (Stripe, GitHub).
 */
@Component
public class TrustedDeviceCookieService {

    public static final String COOKIE_NAME = "device_token";
    public static final String COOKIE_PATH = "/api/v1/auth";

    private final SecurityProperties securityProperties;
    private final long maxAgeSeconds;

    public TrustedDeviceCookieService(
            SecurityProperties securityProperties,
            @Value("${app.auth.trusted-device.expiration-days:30}") long expirationDays) {
        this.securityProperties = securityProperties;
        this.maxAgeSeconds = Duration.ofDays(expirationDays).toSeconds();
    }

    public String issueCookieHeader(String rawToken) {
        // No token to issue (e.g. ADMIN — never trusted): emit a clearing
        // cookie instead of a "device_token=null" one. Idempotent no-op.
        if (rawToken == null || rawToken.isBlank()) {
            return clearCookieHeader();
        }
        return ResponseCookie.from(COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(securityProperties.requireHttps())
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build()
                .toString();
    }

    public String clearCookieHeader() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(securityProperties.requireHttps())
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build()
                .toString();
    }

    /**
     * Reads the device token from the inbound cookie, returning {@code null}
     * when the user has never trusted this browser (or has cleared their
     * cookies, etc.).
     */
    public String readDeviceToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }

    public long trustDurationSeconds() {
        return maxAgeSeconds;
    }
}
