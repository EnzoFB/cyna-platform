package com.cyna.shared.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        boolean requireHttps,
        Hsts hsts,
        String contentSecurityPolicy,
        String referrerPolicy,
        String permissionsPolicy,
        Csrf csrf,
        RateLimit rateLimit
) {
    public record Hsts(
            boolean enabled,
            long maxAgeSeconds,
            boolean includeSubdomains,
            boolean preload
    ) {
    }

    public record Csrf(
            boolean enabled,
            java.util.List<String> ignoredPaths
    ) {
    }

    public record RateLimit(
            boolean enabled,
            int windowSeconds,
            int publicRequestsPerMinute,
            int authenticatedRequestsPerMinute,
            int adminRequestsPerMinute,
            int authRequestsPerMinute
    ) {
    }
}
