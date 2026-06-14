package com.cyna.modules.user.interfaces.rest.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Centralises the attributes of the refresh-token cookie. The refresh token
 * lives in an {@code HttpOnly; Secure; SameSite=Lax} cookie scoped to the
 * auth endpoints, never in localStorage. Three goals:
 *
 *   1. XSS containment — the token is unreachable from any JS context,
 *      so a script injection (Angular SSR misuse, a third-party widget
 *      compromised, an analytics tag turned malicious) cannot exfiltrate
 *      it.
 *   2. CSRF containment — {@code SameSite=Lax} blocks cookies on
 *      cross-origin POST. So /auth/refresh remains immune even though
 *      it is now cookie-authenticated.
 *   3. Path scoping — {@code Path=/api/v1/auth} keeps the cookie out of
 *      every other backend round-trip. The data endpoints continue to
 *      authenticate via the {@code Authorization: Bearer ...} header.
 *
 * Cookie name: {@code refresh_token}. Lifetime aligned with the JWT
 * refresh-token TTL ({@code jwt.refresh-expiration-hours}).
 */
@Component
public class RefreshCookieService {

    public static final String COOKIE_NAME = "refresh_token";
    public static final String COOKIE_PATH = "/api/v1/auth";

    private final boolean requireHttps;
    private final long refreshTokenTtlSeconds;

    public RefreshCookieService(
            @Value("${app.security.require-https:false}") boolean requireHttps,
            @Value("${jwt.refresh-expiration-hours:24}") long refreshTokenTtlHours) {
        this.requireHttps = requireHttps;
        this.refreshTokenTtlSeconds = Duration.ofHours(refreshTokenTtlHours).toSeconds();
    }

    public String issueCookieHeader(String rawRefreshToken) {
        return ResponseCookie.from(COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(isSecureChannel())
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(refreshTokenTtlSeconds)
                .build()
                .toString();
    }

    public String clearCookieHeader() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isSecureChannel())
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build()
                .toString();
    }

    /**
     * Reads the refresh token from the inbound cookie, returning {@code null}
     * if absent. Callers fall back to the legacy JSON body during the
     * deprecation window (so existing tabs holding a localStorage token
     * keep working until they get rotated onto the cookie).
     */
    public String readRefreshToken(HttpServletRequest request) {
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

    /**
     * Helper for controllers to attach the Set-Cookie header on their
     * ResponseEntity without leaking the cookie internals.
     */
    public HttpHeaders cookieHeaders(String setCookieValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, setCookieValue);
        return headers;
    }

    private boolean isSecureChannel() {
        // requireHttps is the existing prod-vs-dev hint: true in any
        // environment where the app sits behind HTTPS, false on the dev
        // box. Browsers reject Secure cookies on plain HTTP except for
        // localhost, so this flag also controls whether we set Secure
        // for local development.
        return requireHttps;
    }
}
