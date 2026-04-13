package com.cyna.shared.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        boolean requireHttps,
        Hsts hsts,
        String contentSecurityPolicy,
        String referrerPolicy,
        String permissionsPolicy
) {
    public record Hsts(
            boolean enabled,
            long maxAgeSeconds,
            boolean includeSubdomains,
            boolean preload
    ) {
    }
}
