package com.cyna.modules.payment.application.query.listpaymentmethods;

import com.cyna.modules.payment.domain.port.PaymentGatewayPort.PaymentMethodSummary;

public record SavedPaymentMethodReadModel(
        // Stripe PaymentMethod id (pm_...). Stripe is the source of truth — we
        // expose its id directly so the UI never diverges from the Stripe Portal.
        String id,
        String stripePaymentMethodId,
        String brand,
        String last4,
        String expMonth,
        String expYear,
        String holderName,
        boolean isDefault
) {
    public static SavedPaymentMethodReadModel fromStripe(PaymentMethodSummary s) {
        return new SavedPaymentMethodReadModel(
                s.stripePaymentMethodId(), s.stripePaymentMethodId(), s.brand(), s.last4(),
                s.expMonth(), s.expYear(), s.holderName(), s.isDefault()
        );
    }
}
