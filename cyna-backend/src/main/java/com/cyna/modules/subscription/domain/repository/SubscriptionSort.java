package com.cyna.modules.subscription.domain.repository;

import com.cyna.shared.domain.Result;

public record SubscriptionSort(SubscriptionSortField field, SubscriptionSortDirection direction) {

    public static SubscriptionSort defaultSort() {
        return new SubscriptionSort(SubscriptionSortField.CREATED_AT, SubscriptionSortDirection.DESC);
    }

    /**
     * Lenient parse for the public API: an unknown or malformed {@code sort}
     * query parameter falls back to the default ordering rather than failing
     * the request.
     */
    public static SubscriptionSort parseOrDefault(String rawSort) {
        Result<SubscriptionSort> parsed = parse(rawSort);
        return parsed.isSuccess() ? parsed.getValue() : defaultSort();
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
