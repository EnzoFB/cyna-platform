package com.cyna.modules.user.interfaces.dto.request;

/**
 * Body schema for /auth/refresh and /auth/logout.
 *
 * <p>{@code refreshToken} is intentionally optional: the cookie-based flow
 * carries the token in the {@code refresh_token} cookie instead, so the
 * body is empty (or null) on those calls. During the deprecation window we
 * still accept the field for legacy clients that haven't migrated off
 * localStorage — the controller falls back to it when no cookie is
 * present.</p>
 */
import com.cyna.shared.validation.NoHtml;

public record RefreshRequest(
        @NoHtml(message = "Refresh token must not contain HTML")
        String refreshToken
) {}
