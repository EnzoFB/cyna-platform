package com.cyna.modules.order.application.query.list;

import com.cyna.shared.domain.Result;

public record OrderSort(OrderSortField field, OrderSortDirection direction) {

    public static OrderSort defaultSort() {
        return new OrderSort(OrderSortField.CREATED_AT, OrderSortDirection.DESC);
    }

    public static Result<OrderSort> parse(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return Result.success(defaultSort());
        }

        String[] parts = rawSort.split(",", 2);
        String rawField = parts[0].trim();
        String rawDirection = parts.length > 1 ? parts[1].trim() : "";

        var fieldResult = OrderSortField.fromExternal(rawField);
        if (fieldResult.isEmpty()) {
            return Result.failure("Invalid sort field");
        }

        OrderSortDirection direction = OrderSortDirection.fromExternal(rawDirection)
                .orElse(rawDirection.isBlank() ? OrderSortDirection.ASC : null);

        if (direction == null) {
            return Result.failure("Invalid sort direction");
        }

        return Result.success(new OrderSort(fieldResult.get(), direction));
    }
}
