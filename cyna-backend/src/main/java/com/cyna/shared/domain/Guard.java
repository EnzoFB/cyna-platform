package com.cyna.shared.domain;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Precondition utility for enforcing invariants in domain objects.
 * All guard methods throw IllegalArgumentException on violation.
 */
public final class Guard {

    private Guard() {}

    public static void againstNull(Object value, String parameterName) {
        if (value == null) {
            throw new IllegalArgumentException(parameterName + " must not be null");
        }
    }

    public static void againstNullOrBlank(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " must not be null or blank");
        }
    }

    public static void againstEmpty(Collection<?> collection, String parameterName) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(parameterName + " must not be empty");
        }
    }

    public static void againstNegativeOrZero(BigDecimal value, String parameterName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(parameterName + " must be positive");
        }
    }

    public static void againstNegative(BigDecimal value, String parameterName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(parameterName + " must not be negative");
        }
    }

    public static void againstNegativeOrZero(int value, String parameterName) {
        if (value <= 0) {
            throw new IllegalArgumentException(parameterName + " must be positive");
        }
    }

    public static void againstOutOfRange(int value, int min, int max, String parameterName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    parameterName + " must be between " + min + " and " + max + ", was: " + value
            );
        }
    }
}
