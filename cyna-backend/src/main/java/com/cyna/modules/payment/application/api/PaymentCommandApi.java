package com.cyna.modules.payment.application.api;

import com.cyna.shared.domain.Result;

public interface PaymentCommandApi {
    Result<Void> cancelStripeSubscription(String stripeSubscriptionId);
}
