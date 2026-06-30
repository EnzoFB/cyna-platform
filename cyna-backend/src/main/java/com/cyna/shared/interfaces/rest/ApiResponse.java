package com.cyna.shared.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standard API response wrapper.
 * ALL API endpoints must return responses wrapped in this type.
 */
@Schema(description = "Standard response envelope wrapping every API payload")
public record ApiResponse<T>(
    @Schema(description = "True when the request succeeded", example = "true")
    boolean success,

    @Schema(description = "Response payload; null when the request failed")
    T data,

    @Schema(description = "Error details; null when the request succeeded")
    ApiError error,

    @Schema(description = "Server timestamp when the response was produced", example = "2026-06-17T10:15:30Z")
    Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message, null), Instant.now());
    }
}
