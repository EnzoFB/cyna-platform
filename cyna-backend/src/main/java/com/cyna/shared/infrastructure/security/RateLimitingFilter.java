package com.cyna.shared.infrastructure.security;

import com.cyna.shared.application.RateLimiter;
import com.cyna.shared.interfaces.rest.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String TOO_MANY_REQUESTS_CODE = "TOO_MANY_REQUESTS";
    private static final String TOO_MANY_REQUESTS_MESSAGE = "Rate limit exceeded. Please retry later.";
    private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private static final int DEFAULT_WINDOW_SECONDS = 60;
    private static final int DEFAULT_PUBLIC_REQUESTS_PER_MINUTE = 60;
    private static final int DEFAULT_AUTHENTICATED_REQUESTS_PER_MINUTE = 300;
    private static final int DEFAULT_ADMIN_REQUESTS_PER_MINUTE = 600;
    private static final int DEFAULT_AUTH_REQUESTS_PER_MINUTE = 20;

    private final InMemoryRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;

    public RateLimitingFilter(InMemoryRateLimiter rateLimiter,
                              ObjectMapper objectMapper,
                              SecurityProperties securityProperties) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        RateLimitConfig config = resolveConfig();
        return !config.enabled() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RateLimitConfig config = resolveConfig();
        RateLimitPolicy policy = resolvePolicy(request, config);
        String bucketKey = buildBucketKey(request, policy);

        RateLimiter.RateLimitDecision decision = rateLimiter.consume(
                bucketKey,
                policy.maxRequestsPerMinute(),
                config.windowSeconds(),
                Instant.now()
        );

        response.setHeader(RATE_LIMIT_LIMIT_HEADER, Integer.toString(decision.limit()));
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, Integer.toString(decision.remaining()));

        if (!decision.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(RETRY_AFTER_HEADER, Long.toString(decision.retryAfterSeconds()));
            objectMapper.writeValue(
                    response.getOutputStream(),
                    ApiResponse.error(TOO_MANY_REQUESTS_CODE, TOO_MANY_REQUESTS_MESSAGE)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitPolicy resolvePolicy(HttpServletRequest request, RateLimitConfig config) {
        String path = request.getRequestURI();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (path.startsWith("/api/v1/auth/")) {
            return new RateLimitPolicy("auth", config.authRequestsPerMinute());
        }
        if (isAdmin(authentication)) {
            return new RateLimitPolicy("admin", config.adminRequestsPerMinute());
        }
        if (isAuthenticated(authentication)) {
            return new RateLimitPolicy("authenticated", config.authenticatedRequestsPerMinute());
        }

        return new RateLimitPolicy("public", config.publicRequestsPerMinute());
    }

    private String buildBucketKey(HttpServletRequest request, RateLimitPolicy policy) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isAuthenticated(authentication)) {
            return policy.category() + ":user:" + authentication.getName();
        }
        return policy.category() + ":ip:" + extractClientIp(request);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String firstForwardedIp = forwardedFor.split(",")[0].trim();
            if (!firstForwardedIp.isBlank()) {
                return firstForwardedIp;
            }
        }
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress != null && !remoteAddress.isBlank() ? remoteAddress : "unknown";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    private boolean isAdmin(Authentication authentication) {
        if (!isAuthenticated(authentication) || authentication.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private RateLimitConfig resolveConfig() {
        SecurityProperties.RateLimit configured = securityProperties.rateLimit();
        if (configured == null) {
            return RateLimitConfig.defaults();
        }

        return new RateLimitConfig(
                configured.enabled(),
                configured.windowSeconds() > 0 ? configured.windowSeconds() : DEFAULT_WINDOW_SECONDS,
                configured.publicRequestsPerMinute() > 0 ? configured.publicRequestsPerMinute()
                        : DEFAULT_PUBLIC_REQUESTS_PER_MINUTE,
                configured.authenticatedRequestsPerMinute() > 0 ? configured.authenticatedRequestsPerMinute()
                        : DEFAULT_AUTHENTICATED_REQUESTS_PER_MINUTE,
                configured.adminRequestsPerMinute() > 0 ? configured.adminRequestsPerMinute()
                        : DEFAULT_ADMIN_REQUESTS_PER_MINUTE,
                configured.authRequestsPerMinute() > 0 ? configured.authRequestsPerMinute()
                        : DEFAULT_AUTH_REQUESTS_PER_MINUTE
        );
    }

    private record RateLimitConfig(
            boolean enabled,
            long windowSeconds,
            int publicRequestsPerMinute,
            int authenticatedRequestsPerMinute,
            int adminRequestsPerMinute,
            int authRequestsPerMinute
    ) {
        private static RateLimitConfig defaults() {
            return new RateLimitConfig(
                    true,
                    DEFAULT_WINDOW_SECONDS,
                    DEFAULT_PUBLIC_REQUESTS_PER_MINUTE,
                    DEFAULT_AUTHENTICATED_REQUESTS_PER_MINUTE,
                    DEFAULT_ADMIN_REQUESTS_PER_MINUTE,
                    DEFAULT_AUTH_REQUESTS_PER_MINUTE
            );
        }
    }

    private record RateLimitPolicy(String category, int maxRequestsPerMinute) {
    }
}
