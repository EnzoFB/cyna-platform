package com.cyna.modules.order.application.query.list;

import java.util.Locale;
import java.util.Optional;

public enum OrderSortDirection {
    ASC,
    DESC;

    public static Optional<OrderSortDirection> fromExternal(String rawDirection) {
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
