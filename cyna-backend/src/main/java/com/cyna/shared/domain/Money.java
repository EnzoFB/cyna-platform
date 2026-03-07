package com.cyna.shared.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a monetary amount with currency.
 * Used across multiple modules (product pricing, order totals).
 */
public record Money(BigDecimal amount, String currency) {

    public static final Money ZERO_EUR = new Money(BigDecimal.ZERO, "EUR");

    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        }
        Guard.againstNullOrBlank(currency, "currency");
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies: " + this.currency + " and " + other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }
}
