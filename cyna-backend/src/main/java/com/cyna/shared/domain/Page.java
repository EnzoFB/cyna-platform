package com.cyna.shared.domain;

import java.util.List;

/**
 * Framework-free pagination model for the domain/application layer.
 *
 * @param <T> the type of items in the page
 */
public record Page<T>(
    List<T> items,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
) {
    public boolean hasNext() {
        return pageNumber < totalPages - 1;
    }

    public boolean hasPrevious() {
        return pageNumber > 0;
    }
}
