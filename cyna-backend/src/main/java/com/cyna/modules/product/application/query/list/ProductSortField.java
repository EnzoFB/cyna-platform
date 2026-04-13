package com.cyna.modules.product.application.query.list;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum ProductSortField {
    NAME(List.of("name"), "name"),
    PRICE(List.of("price", "monthlyPrice"), "monthlyPrice"),
    ANNUAL_PRICE(List.of("annualPrice"), "annualPrice"),
    STATUS(List.of("status"), "status"),
    PRIORITY(List.of("priority"), "priority"),
    CREATED_AT(List.of("createdAt"), "createdAt");

    private final List<String> externalNames;
    private final String jpaProperty;

    ProductSortField(List<String> externalNames, String jpaProperty) {
        this.externalNames = externalNames;
        this.jpaProperty = jpaProperty;
    }

    public String jpaProperty() {
        return jpaProperty;
    }

    public static Optional<ProductSortField> fromExternal(String rawField) {
        if (rawField == null || rawField.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawField.trim().toLowerCase(Locale.ROOT);
        for (ProductSortField field : values()) {
            for (String external : field.externalNames) {
                if (external.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return Optional.of(field);
                }
            }
        }
        return Optional.empty();
    }
}
