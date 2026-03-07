package com.cyna.shared.interfaces.rest;

import java.util.List;

/**
 * Structured error payload for API error responses.
 */
public record ApiError(
    String code,
    String message,
    List<FieldError> details
) {}
