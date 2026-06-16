package com.cyna.modules.payment.application.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result of {@code POST /payments/initiate}. The frontend uses
 * {@code setupIntentClientSecret} with Stripe PaymentElement (in setup mode)
 * to collect a card, then calls {@code POST /payments/finalize} with the
 * resulting PaymentMethod id.
 */
public record PaymentInitiatedReadModel(
        UUID paymentId,
        UUID orderId,
        String setupIntentClientSecret,
        BigDecimal amount,
        String currency
) {}
