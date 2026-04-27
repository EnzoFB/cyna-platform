package com.cyna.modules.product.application.query.list;

import com.cyna.shared.domain.Result;

public record ProductSort(ProductSortField field, SortDirection direction) {

    public static ProductSort defaultSort() {
        return new ProductSort(ProductSortField.PRIORITY, SortDirection.DESC);
    }

    public static Result<ProductSort> parse(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return Result.success(defaultSort());
        }

        String[] parts = rawSort.split(",", 2);
        String rawField = parts[0].trim();
        String rawDirection = parts.length > 1 ? parts[1].trim() : "";

        var fieldResult = ProductSortField.fromExternal(rawField);
        if (fieldResult.isEmpty()) {
            return Result.failure("Invalid sort field: " + rawField);
        }

        SortDirection direction = SortDirection.fromExternal(rawDirection)
                .orElse(rawDirection.isBlank() ? SortDirection.ASC : null);

        if (direction == null) {
            return Result.failure("Invalid sort direction: " + rawDirection);
        }

        return Result.success(new ProductSort(fieldResult.get(), direction));
    }
}
