package com.cyna.modules.order.domain.repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum OrderSortField {
    CREATED_AT(List.of("createdAt"), "createdAt"),
    TOTAL_AMOUNT(List.of("totalAmount"), "totalAmount"),
    STATUS(List.of("status"), "status");

    private final List<String> externalNames;
    private final String jpaProperty;

    OrderSortField(List<String> externalNames, String jpaProperty) {
        this.externalNames = externalNames;
        this.jpaProperty = jpaProperty;
    }

    public String jpaProperty() {
        return jpaProperty;
    }

    public static Optional<OrderSortField> fromExternal(String rawField) {
        if (rawField == null || rawField.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawField.trim().toLowerCase(Locale.ROOT);
        for (OrderSortField field : values()) {
            for (String external : field.externalNames) {
                if (external.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return Optional.of(field);
                }
            }
        }
        return Optional.empty();
    }
}
