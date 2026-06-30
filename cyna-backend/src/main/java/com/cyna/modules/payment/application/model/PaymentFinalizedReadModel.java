package com.cyna.modules.payment.application.model;

import java.util.List;
import java.util.UUID;

/**
 * Result of {@code POST /payments/finalize}. Three possible shapes:
 *
 * <ul>
 *   <li><b>Settled</b> ({@code requiresAction=false}, {@code lines} populated,
 *       {@code pendingActions} empty) — every line was charged; the front
 *       redirects to the order confirmation page.</li>
 *   <li><b>Requires SCA</b> ({@code requiresAction=true}, {@code pendingActions}
 *       populated) — at least one off-session charge triggered a PSD2 3DS
 *       challenge. The front must call
 *       {@code stripe.confirmCardPayment(action.clientSecret)} for each entry,
 *       then poll/await the subscription's {@code active} status (driven by
 *       {@code customer.subscription.updated} webhooks). Payment remains
 *       PENDING locally; do NOT roll back the Stripe subscriptions — they are
 *       recoverable.</li>
 *   <li><b>Declined</b> — not returned through this read model; the handler
 *       surfaces {@code Result.failure("PAYMENT_DECLINED")} instead.</li>
 * </ul>
 */
public record PaymentFinalizedReadModel(
        UUID paymentId,
        UUID orderId,
        boolean requiresAction,
        List<FinalizedLine> lines,
        List<PendingAction> pendingActions
) {
    public static PaymentFinalizedReadModel settled(UUID paymentId, UUID orderId, List<FinalizedLine> lines) {
        return new PaymentFinalizedReadModel(paymentId, orderId, false, lines, List.of());
    }

    public static PaymentFinalizedReadModel requiresAction(UUID paymentId, UUID orderId, List<PendingAction> pendingActions) {
        return new PaymentFinalizedReadModel(paymentId, orderId, true, List.of(), pendingActions);
    }

    /**
     * One entry per OrderLine. {@code stripeStatus} mirrors Stripe's
     * subscription status right after creation: {@code "active"} or
     * {@code "trialing"}.
     */
    public record FinalizedLine(
            UUID orderLineId,
            UUID subscriptionId,
            String stripeSubscriptionId,
            String stripeStatus
    ) {}

    /**
     * 3DS challenge to be resolved by the frontend with
     * {@code stripe.confirmCardPayment(clientSecret)}. {@code subscriptionId}
     * is the Stripe Subscription whose first invoice is pending SCA.
     */
    public record PendingAction(
            String stripeSubscriptionId,
            String paymentIntentClientSecret
    ) {}
}
