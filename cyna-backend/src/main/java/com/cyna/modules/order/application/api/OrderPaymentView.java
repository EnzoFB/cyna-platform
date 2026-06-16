package com.cyna.modules.order.application.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payment-side view of an order. Only the HT subtotal is exposed — VAT and
 * the TTC charged to the customer are determined by Stripe Tax at subscription
 * creation and live exclusively on the Stripe invoice.
 */
public record OrderPaymentView(
        UUID id,
        UUID userId,
        String status,
        BigDecimal subtotalHt,
        String currency,
        List<OrderLineView> lines
) {
    public record OrderLineView(
            UUID id,
            UUID productId,
            String productName,
            String productCategory,
            String billingCycle,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
