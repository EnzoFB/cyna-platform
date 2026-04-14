package com.cyna.modules.subscription.application.query.list;

import java.util.Locale;
import java.util.Optional;

public enum SubscriptionSortDirection {
    ASC,
    DESC;

    public static Optional<SubscriptionSortDirection> fromExternal(String rawDirection) {
        if (rawDirection == null || rawDirection.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawDirection.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "asc" -> Optional.of(ASC);
            case "desc" -> Optional.of(DESC);
            default -> Optional.empty();
        };
    }
}
