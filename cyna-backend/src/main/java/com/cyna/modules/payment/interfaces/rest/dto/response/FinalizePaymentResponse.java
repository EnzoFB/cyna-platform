package com.cyna.modules.payment.interfaces.rest.dto.response;

import com.cyna.modules.payment.application.model.PaymentFinalizedReadModel;

import java.util.List;
import java.util.UUID;

public record FinalizePaymentResponse(
        UUID paymentId,
        UUID orderId,
        List<FinalizedLine> lines
) {
    public record FinalizedLine(
            UUID orderLineId,
            UUID subscriptionId,
            String stripeSubscriptionId,
            String stripeStatus
    ) {}

    public static FinalizePaymentResponse from(PaymentFinalizedReadModel model) {
        return new FinalizePaymentResponse(
                model.paymentId(),
                model.orderId(),
                model.lines().stream()
                        .map(l -> new FinalizedLine(
                                l.orderLineId(),
                                l.subscriptionId(),
                                l.stripeSubscriptionId(),
                                l.stripeStatus()
                        ))
                        .toList()
        );
    }
}
