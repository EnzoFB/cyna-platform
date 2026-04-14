package com.cyna.modules.subscription.application.api;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.shared.domain.Result;

public interface SubscriptionCommandApi {
    Result<SubscriptionReadModel> createFromPayment(SubscriptionPaymentPayload payload);
}
