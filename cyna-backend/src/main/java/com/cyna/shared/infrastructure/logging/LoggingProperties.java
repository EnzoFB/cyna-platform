package com.cyna.shared.infrastructure.logging;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.logging")
public record LoggingProperties(
    Boolean requestLoggingEnabled,
    Boolean maskSensitiveData,
    Set<String> sensitiveHeaders,
    Set<String> maskedBodyPatterns
) {
    public LoggingProperties {
        if (requestLoggingEnabled == null) requestLoggingEnabled = false;
        if (maskSensitiveData == null) maskSensitiveData = true;
        if (sensitiveHeaders == null) sensitiveHeaders = Set.of("Authorization", "Cookie", "X-Api-Key", "Stripe-Signature");
        if (maskedBodyPatterns == null) maskedBodyPatterns = Set.of("password", "cardNumber", "cvv", "token", "secretKey", "secret");
    }
}
