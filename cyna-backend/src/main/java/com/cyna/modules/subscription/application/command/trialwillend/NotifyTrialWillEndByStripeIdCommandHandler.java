package com.cyna.modules.subscription.application.command.trialwillend;

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
public class NotifyTrialWillEndByStripeIdCommandHandler
        implements CommandHandler<NotifyTrialWillEndByStripeIdCommand, Void> {

    private static final Logger log =
            LoggerFactory.getLogger(NotifyTrialWillEndByStripeIdCommandHandler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public NotifyTrialWillEndByStripeIdCommandHandler(
            SubscriptionRepository subscriptionRepository,
            DomainEventPublisher eventPublisher,
            TransactionRunner transactionRunner) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(NotifyTrialWillEndByStripeIdCommand command) {
        return transactionRunner.runReturning(() -> {
            List<Subscription> subscriptions =
                    subscriptionRepository.findAllByStripeSubscriptionId(command.stripeSubscriptionId());

            if (subscriptions.isEmpty()) {
                log.warn("trial_will_end event for unknown stripe subscription {}",
                        command.stripeSubscriptionId());
                return Result.success();
            }

            // Pure signal — no state mutation, so nothing to persist. We only
            // raise + publish the event; the notification module sends the email.
            for (Subscription subscription : subscriptions) {
                Result<Subscription> updated = subscription.notifyTrialWillEnd(command.trialEndAt());
                if (updated.isFailure()) {
                    log.warn("Cannot emit trial-will-end for subscription {}: {}",
                            subscription.getId(), updated.getError());
                    continue;
                }
                eventPublisher.publishAll(updated.getValue().getDomainEvents());
                updated.getValue().clearDomainEvents();
            }

            return Result.success();
        });
    }
}
