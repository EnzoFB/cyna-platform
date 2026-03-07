package com.cyna.shared.interfaces.rest;

/**
 * Field-level validation error detail.
 */
public record FieldError(
    String field,
    String message
) {}
