package com.cyna.modules.product.interfaces.rest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared helpers for building product list queries, reused by the public
 * {@link ProductController} and the {@link AdminProductController}.
 */
final class ProductQuerySupport {

    private ProductQuerySupport() {
    }

    static List<UUID> normalizeCategoryIds(List<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }

        List<UUID> normalized = categoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        return normalized.isEmpty() ? null : normalized;
    }
}
