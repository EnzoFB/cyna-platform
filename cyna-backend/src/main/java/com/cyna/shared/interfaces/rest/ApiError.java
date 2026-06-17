package com.cyna.shared.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Structured error payload for API error responses.
 */
@Schema(description = "Structured error payload returned when a request fails")
public record ApiError(
    @Schema(description = "Machine-readable error code", example = "NOT_FOUND")
    String code,

    @Schema(description = "Human-readable error message", example = "Product not found: 3f1a…")
    String message,

    @Schema(description = "Per-field validation errors; null when not applicable")
    List<FieldError> details
) {}
