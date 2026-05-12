package com.cyna.modules.payment.application.model;

import java.util.List;
import java.util.UUID;

/**
 * Result of {@code POST /payments/finalize} — every OrderLine has been turned
 * into its own Stripe Subscription + local Subscription. The frontend uses
 * this to redirect to the order confirmation page and surface any
 * subscriptions that ended up in a non-active Stripe status (e.g. 3DS or
 * card declined → {@code incomplete}), which the customer must address.
 */
public record PaymentFinalizedReadModel(
        UUID paymentId,
        UUID orderId,
        List<FinalizedLine> lines
) {
    /**
     * One entry per OrderLine. {@code stripeStatus} mirrors Stripe's
     * subscription status right after creation: {@code "active"},
     * {@code "trialing"}, or {@code "incomplete"} when the first invoice
     * could not be charged inline.
     */
    public record FinalizedLine(
            UUID orderLineId,
            UUID subscriptionId,
            String stripeSubscriptionId,
            String stripeStatus
    ) {}
}
