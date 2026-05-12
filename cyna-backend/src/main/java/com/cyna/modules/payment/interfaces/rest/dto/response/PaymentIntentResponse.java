package com.cyna.modules.payment.interfaces.rest.dto.response;

import com.cyna.modules.payment.application.model.PaymentInitiatedReadModel;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response shape for {@code POST /payments/initiate}. The frontend takes the
 * {@code setupIntentClientSecret} and plugs it into Stripe PaymentElement (in
 * setup mode) to collect a card, then calls {@code /payments/finalize} with
 * the resulting PaymentMethod id.
 */
public record PaymentIntentResponse(
        UUID paymentId,
        UUID orderId,
        String setupIntentClientSecret,
        BigDecimal amount,
        String currency
) {
    public static PaymentIntentResponse from(PaymentInitiatedReadModel model) {
        return new PaymentIntentResponse(
                model.paymentId(),
                model.orderId(),
                model.setupIntentClientSecret(),
                model.amount(),
                model.currency()
        );
    }
}
