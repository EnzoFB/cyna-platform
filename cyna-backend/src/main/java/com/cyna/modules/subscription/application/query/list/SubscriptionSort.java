package com.cyna.modules.subscription.application.query.list;

import com.cyna.shared.domain.Result;

public record SubscriptionSort(SubscriptionSortField field, SubscriptionSortDirection direction) {

    public static SubscriptionSort defaultSort() {
        return new SubscriptionSort(SubscriptionSortField.CREATED_AT, SubscriptionSortDirection.DESC);
    }

    public static Result<SubscriptionSort> parse(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return Result.success(defaultSort());
        }

        String[] parts = rawSort.split(",", 2);
        String rawField = parts[0].trim();
        String rawDirection = parts.length > 1 ? parts[1].trim() : "";

        var fieldResult = SubscriptionSortField.fromExternal(rawField);
        if (fieldResult.isEmpty()) {
            return Result.failure("Invalid sort field");
        }

        SubscriptionSortDirection direction = SubscriptionSortDirection.fromExternal(rawDirection)
                .orElse(rawDirection.isBlank() ? SubscriptionSortDirection.ASC : null);

        if (direction == null) {
            return Result.failure("Invalid sort direction");
        }

        return Result.success(new SubscriptionSort(fieldResult.get(), direction));
    }
}
