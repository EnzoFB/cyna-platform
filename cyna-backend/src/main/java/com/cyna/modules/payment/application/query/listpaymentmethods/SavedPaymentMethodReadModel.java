package com.cyna.modules.payment.application.query.listpaymentmethods;

import com.cyna.modules.payment.domain.model.SavedPaymentMethod;

import java.util.UUID;

public record SavedPaymentMethodReadModel(
        UUID id,
        String stripePaymentMethodId,
        String brand,
        String last4,
        String expMonth,
        String expYear,
        String holderName,
        boolean isDefault
) {
    public static SavedPaymentMethodReadModel from(SavedPaymentMethod m) {
        return new SavedPaymentMethodReadModel(
                m.getId(), m.getStripePaymentMethodId(), m.getBrand(), m.getLast4(),
                m.getExpMonth(), m.getExpYear(), m.getHolderName(), m.isDefault()
        );
    }
}
