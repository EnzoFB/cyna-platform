package com.cyna.modules.subscription.application.api;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.shared.domain.Result;

import java.time.Instant;

public interface SubscriptionCommandApi {
    Result<SubscriptionReadModel> createFromPayment(SubscriptionPaymentPayload payload);
    Result<Void> renewByStripeId(String stripeSubscriptionId, Instant newPeriodEnd);
    Result<Void> markPastDueByStripeId(String stripeSubscriptionId);
    Result<Void> cancelByStripeId(String stripeSubscriptionId);
}
