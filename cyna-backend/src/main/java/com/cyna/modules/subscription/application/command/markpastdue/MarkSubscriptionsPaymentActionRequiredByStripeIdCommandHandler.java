package com.cyna.modules.subscription.application.command.markpastdue;

import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarkSubscriptionsPaymentActionRequiredByStripeIdCommandHandler
        implements CommandHandler<MarkSubscriptionsPaymentActionRequiredByStripeIdCommand, Void> {

    private static final Logger log =
            LoggerFactory.getLogger(MarkSubscriptionsPaymentActionRequiredByStripeIdCommandHandler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public MarkSubscriptionsPaymentActionRequiredByStripeIdCommandHandler(
            SubscriptionRepository subscriptionRepository,
            DomainEventPublisher eventPublisher,
            TransactionRunner transactionRunner) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(MarkSubscriptionsPaymentActionRequiredByStripeIdCommand command) {
        return transactionRunner.runReturning(() -> {
            List<Subscription> subscriptions =
                    subscriptionRepository.findAllByStripeSubscriptionId(command.stripeSubscriptionId());

            if (subscriptions.isEmpty()) {
                log.warn("payment_action_required event for unknown stripe subscription {}",
                        command.stripeSubscriptionId());
                return Result.success();
            }

            for (Subscription subscription : subscriptions) {
                Result<Subscription> updated =
                        subscription.markPaymentActionRequired(command.hostedInvoiceUrl());
                if (updated.isFailure()) {
                    log.warn("Cannot mark subscription {} as payment-action-required: {}",
                            subscription.getId(), updated.getError());
                    continue;
                }
                subscriptionRepository.save(updated.getValue());
                eventPublisher.publishAll(updated.getValue().getDomainEvents());
                updated.getValue().clearDomainEvents();
            }

            return Result.success();
        });
    }
}
