package com.cyna.modules.product.domain.repository;

import com.cyna.shared.domain.Result;

public record ProductSort(ProductSortField field, SortDirection direction) {

    public static ProductSort defaultSort() {
        return new ProductSort(ProductSortField.PRIORITY, SortDirection.DESC);
    }

    /**
     * Lenient parse for the public API: an unknown or malformed {@code sort}
     * query parameter falls back to the default ordering rather than failing
     * the request.
     */
    public static ProductSort parseOrDefault(String rawSort) {
        Result<ProductSort> parsed = parse(rawSort);
        return parsed.isSuccess() ? parsed.getValue() : defaultSort();
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
            return Result.failure("Invalid sort field");
        }

        SortDirection direction = SortDirection.fromExternal(rawDirection)
                .orElse(rawDirection.isBlank() ? SortDirection.ASC : null);

        if (direction == null) {
            return Result.failure("Invalid sort direction");
        }

        return Result.success(new ProductSort(fieldResult.get(), direction));
    }
}
