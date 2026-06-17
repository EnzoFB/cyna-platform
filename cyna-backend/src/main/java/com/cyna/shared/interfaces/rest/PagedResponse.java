package com.cyna.shared.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Standard paginated response DTO for API list endpoints.
 */
@Schema(description = "Paginated list payload returned by collection endpoints")
public record PagedResponse<T>(
    @Schema(description = "Items on the current page")
    List<T> items,

    @Schema(description = "Zero-based index of the current page", example = "0")
    int page,

    @Schema(description = "Requested page size", example = "20")
    int size,

    @Schema(description = "Total number of items across all pages", example = "137")
    long totalElements,

    @Schema(description = "Total number of pages", example = "7")
    int totalPages,

    @Schema(description = "True when this is the first page", example = "true")
    boolean first,

    @Schema(description = "True when this is the last page", example = "false")
    boolean last
) {
    public static <T> PagedResponse<T> of(List<T> items, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PagedResponse<>(
                items,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );
    }
}
