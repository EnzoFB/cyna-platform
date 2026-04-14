package com.cyna.modules.subscription.application.query.list;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum SubscriptionSortField {
    CREATED_AT(List.of("createdAt"), "createdAt"),
    START_AT(List.of("startAt"), "startAt"),
    END_AT(List.of("endAt"), "endAt"),
    STATUS(List.of("status"), "status");

    private final List<String> externalNames;
    private final String jpaProperty;

    SubscriptionSortField(List<String> externalNames, String jpaProperty) {
        this.externalNames = externalNames;
        this.jpaProperty = jpaProperty;
    }

    public String jpaProperty() {
        return jpaProperty;
    }

    public static Optional<SubscriptionSortField> fromExternal(String rawField) {
        if (rawField == null || rawField.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawField.trim().toLowerCase(Locale.ROOT);
        for (SubscriptionSortField field : values()) {
            for (String external : field.externalNames) {
                if (external.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return Optional.of(field);
                }
            }
        }
        return Optional.empty();
    }
}
