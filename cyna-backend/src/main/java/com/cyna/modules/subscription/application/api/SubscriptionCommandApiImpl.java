package com.cyna.modules.subscription.application.api;

import com.cyna.modules.subscription.application.command.cancel.CancelSubscriptionsByStripeIdCommand;
import com.cyna.modules.subscription.application.command.create.CreateSubscriptionCommand;
import com.cyna.modules.subscription.application.command.markpastdue.MarkSubscriptionsPastDueByStripeIdCommand;
import com.cyna.modules.subscription.application.command.renew.RenewSubscriptionsByStripeIdCommand;
import com.cyna.modules.subscription.application.command.sync.SyncSubscriptionsFromStripeCommand;
import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.shared.application.Mediator;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Service;

import java.time.Instant;

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
                payload.nextBillingAt(),
                payload.stripeSubscriptionId(),
                payload.stripeScheduleId()
        );
        return mediator.send(command);
    }

    @Override
    public Result<Void> renewByStripeId(String stripeSubscriptionId, Instant newPeriodEnd) {
        return mediator.send(new RenewSubscriptionsByStripeIdCommand(stripeSubscriptionId, newPeriodEnd));
    }

    @Override
    public Result<Void> markPastDueByStripeId(String stripeSubscriptionId) {
        return mediator.send(new MarkSubscriptionsPastDueByStripeIdCommand(stripeSubscriptionId));
    }

    @Override
    public Result<Void> cancelByStripeId(String stripeSubscriptionId) {
        return mediator.send(new CancelSubscriptionsByStripeIdCommand(stripeSubscriptionId));
    }

    @Override
    public Result<Void> syncFromStripeState(String stripeSubscriptionId,
                                            String stripeStatus,
                                            Boolean cancelAtPeriodEnd,
                                            Instant currentPeriodEnd,
                                            Instant stripeCanceledAt) {
        return mediator.send(new SyncSubscriptionsFromStripeCommand(
                stripeSubscriptionId,
                stripeStatus,
                cancelAtPeriodEnd,
                currentPeriodEnd,
                stripeCanceledAt
        ));
    }
}
