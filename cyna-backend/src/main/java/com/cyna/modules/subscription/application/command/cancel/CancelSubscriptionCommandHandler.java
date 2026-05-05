package com.cyna.modules.subscription.application.command.cancel;

import com.cyna.modules.payment.application.api.PaymentCommandApi;
import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class CancelSubscriptionCommandHandler implements CommandHandler<CancelSubscriptionCommand, SubscriptionReadModel> {

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRunner transactionRunner;
    private final DomainEventPublisher eventPublisher;
    private final PaymentCommandApi paymentCommandApi;

    public CancelSubscriptionCommandHandler(SubscriptionRepository subscriptionRepository,
                                            TransactionRunner transactionRunner,
                                            DomainEventPublisher eventPublisher,
                                            PaymentCommandApi paymentCommandApi) {
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRunner = transactionRunner;
        this.eventPublisher = eventPublisher;
        this.paymentCommandApi = paymentCommandApi;
    }

    @Override
    public Result<SubscriptionReadModel> handle(CancelSubscriptionCommand command) {
        // 1. Pre-flight read (no transaction): fetch the subscription to check ownership
        //    and grab the Stripe subscription id so we can cancel it before holding any DB lock.
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElse(null);
        if (subscription == null) {
            return Result.failure("Subscription not found: " + command.subscriptionId());
        }
        if (!subscription.getUserId().equals(command.userId())) {
            return Result.failure("Access denied");
        }

        // 2. Cancel the subscription at Stripe FIRST so we can never end up in a state
        //    where the customer keeps getting billed after a successful local cancel.
        //    Idempotent: if already cancelled at Stripe, this is a no-op.
        if (subscription.getStripeSubscriptionId() != null) {
            Result<Void> stripeResult = paymentCommandApi
                    .cancelStripeSubscription(subscription.getStripeSubscriptionId());
            if (stripeResult.isFailure()) {
                return Result.failure(stripeResult.getError());
            }
        }

        // 3. Persist the local cancellation in a transaction.
        return transactionRunner.runReturning(() -> {
            Subscription latest = subscriptionRepository.findById(command.subscriptionId())
                    .orElse(null);
            if (latest == null) {
                return Result.failure("Subscription not found: " + command.subscriptionId());
            }

            Result<Subscription> cancelledResult = latest.cancelAtPeriodEnd();
            if (cancelledResult.isFailure()) {
                // Already cancelled locally (e.g. webhook arrived first) — return current state
                return Result.success(SubscriptionReadModel.from(latest));
            }

            Subscription cancelled = cancelledResult.getValue();
            subscriptionRepository.save(cancelled);
            eventPublisher.publishAll(cancelled.getDomainEvents());
            cancelled.clearDomainEvents();

            return Result.success(SubscriptionReadModel.from(cancelled));
        });
    }
}
