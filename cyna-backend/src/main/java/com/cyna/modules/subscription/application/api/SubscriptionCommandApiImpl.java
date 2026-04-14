package com.cyna.modules.subscription.application.api;

import com.cyna.modules.subscription.application.command.create.CreateSubscriptionCommand;
import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Service;

@Service
class SubscriptionCommandApiImpl implements SubscriptionCommandApi {

    private final Mediator mediator;

    SubscriptionCommandApiImpl(Mediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public Result<SubscriptionReadModel> createFromPayment(SubscriptionPaymentPayload payload) {
        var command = new CreateSubscriptionCommand(
                payload.userId(),
                payload.orderId(),
                payload.productId(),
                payload.productName(),
                payload.productCategory(),
                payload.billingCycle(),
                payload.quantity(),
                payload.unitPrice(),
                payload.currency(),
                payload.startAt(),
                payload.endAt(),
                payload.nextBillingAt()
        );
        return mediator.send(command);
    }
}
