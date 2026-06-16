package com.cyna.modules.payment.interfaces.rest.dto.response;

import com.cyna.modules.payment.application.model.PaymentFinalizedReadModel;

import java.util.List;
import java.util.UUID;

/**
 * Response of {@code POST /payments/finalize}. {@code requiresAction=true}
 * means the off-session charge triggered a PSD2 3DS challenge — the front
 * MUST call {@code stripe.confirmCardPayment(action.clientSecret)} for every
 * entry in {@code pendingActions} before treating the order as paid. The
 * controller still returns HTTP 200 in this case: the payment is recoverable,
 * not declined.
 */
public record FinalizePaymentResponse(
        UUID paymentId,
        UUID orderId,
        boolean requiresAction,
        List<FinalizedLine> lines,
        List<PendingAction> pendingActions
) {
    public record FinalizedLine(
            UUID orderLineId,
            UUID subscriptionId,
            String stripeSubscriptionId,
            String stripeStatus
    ) {}

    public record PendingAction(
            String stripeSubscriptionId,
            String paymentIntentClientSecret
    ) {}

    public static FinalizePaymentResponse from(PaymentFinalizedReadModel model) {
        return new FinalizePaymentResponse(
                model.paymentId(),
                model.orderId(),
                model.requiresAction(),
                model.lines().stream()
                        .map(l -> new FinalizedLine(
                                l.orderLineId(),
                                l.subscriptionId(),
                                l.stripeSubscriptionId(),
                                l.stripeStatus()
                        ))
                        .toList(),
                model.pendingActions().stream()
                        .map(a -> new PendingAction(
                                a.stripeSubscriptionId(),
                                a.paymentIntentClientSecret()
                        ))
                        .toList()
        );
    }
}
