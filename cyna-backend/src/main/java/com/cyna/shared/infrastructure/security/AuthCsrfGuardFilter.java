package com.cyna.shared.infrastructure.security;

import com.cyna.shared.interfaces.rest.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Browser-side CSRF guard for auth endpoints that can be hit with cookies.
 *
 * <p>The API stays stateless (JWT bearer for protected routes), but /auth/*
 * uses HttpOnly cookies (refresh/device). This filter blocks cross-site unsafe
 * requests by validating Fetch Metadata and request provenance headers against
 * configured frontend origins.</p>
 */
@Component
public class AuthCsrfGuardFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
    private static final Set<String> UNSAFE_METHODS = Set.of(
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name()
    );
    private static final String FORBIDDEN_CODE = "FORBIDDEN";
    private static final String FORBIDDEN_MESSAGE = "Cross-site request blocked";

    private final ObjectMapper objectMapper;
    private final Set<String> allowedOrigins;

    public AuthCsrfGuardFilter(
            ObjectMapper objectMapper,
            @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:4201}") List<String> configuredOrigins) {
        this.objectMapper = objectMapper;
        this.allowedOrigins = normalizeOrigins(configuredOrigins);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return !UNSAFE_METHODS.contains(method) || path == null || !path.startsWith(AUTH_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isRejectedByFetchMetadata(request) || isRejectedByOriginOrReferer(request)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(FORBIDDEN_CODE, FORBIDDEN_MESSAGE));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRejectedByFetchMetadata(HttpServletRequest request) {
        String site = trimToNull(request.getHeader("Sec-Fetch-Site"));
        if (site == null) {
            return false;
        }
        return "cross-site".equalsIgnoreCase(site);
    }

    private boolean isRejectedByOriginOrReferer(HttpServletRequest request) {
        String origin = trimToNull(request.getHeader("Origin"));
        if (origin != null) {
            return !isAllowedOrigin(origin, request);
        }

        String referer = trimToNull(request.getHeader("Referer"));
        if (referer != null) {
            String refererOrigin = extractOrigin(referer);
            return refererOrigin == null || !isAllowedOrigin(refererOrigin, request);
        }

        // Non-browser clients may not send Origin/Referer; keep compatibility.
        return false;
    }

    private boolean isAllowedOrigin(String originValue, HttpServletRequest request) {
        if ("null".equalsIgnoreCase(originValue)) {
            return false;
        }

        String normalized = normalizeOrigin(originValue);
        if (normalized == null) {
            return false;
        }

        if (allowedOrigins.contains(normalized)) {
            return true;
        }

        String requestOrigin = requestOrigin(request);
        return requestOrigin != null && requestOrigin.equalsIgnoreCase(normalized);
    }

    private static Set<String> normalizeOrigins(List<String> configuredOrigins) {
        if (configuredOrigins == null || configuredOrigins.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : configuredOrigins) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(AuthCsrfGuardFilter::normalizeOrigin)
                    .filter(s -> s != null)
                    .forEach(normalized::add);
        }
        return normalized;
    }

    private static String normalizeOrigin(String candidate) {
        try {
            URI uri = URI.create(candidate.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            if (port == -1) {
                return scheme + "://" + host;
            }
            if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
                return scheme + "://" + host;
            }
            return scheme + "://" + host + ":" + port;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String extractOrigin(String absoluteUrl) {
        String normalized = normalizeOrigin(absoluteUrl);
        return normalized;
    }

    private static String requestOrigin(HttpServletRequest request) {
        String scheme = trimToNull(request.getScheme());
        String host = trimToNull(request.getServerName());
        if (scheme == null || host == null) {
            return null;
        }
        int port = request.getServerPort();
        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (port <= 0
                || ("http".equals(normalizedScheme) && port == 80)
                || ("https".equals(normalizedScheme) && port == 443)) {
            return normalizedScheme + "://" + normalizedHost;
        }
        return normalizedScheme + "://" + normalizedHost + ":" + port;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
