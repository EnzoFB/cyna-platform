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
public class MarkSubscriptionsPastDueByStripeIdCommandHandler
        implements CommandHandler<MarkSubscriptionsPastDueByStripeIdCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(MarkSubscriptionsPastDueByStripeIdCommandHandler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public MarkSubscriptionsPastDueByStripeIdCommandHandler(SubscriptionRepository subscriptionRepository,
                                                              DomainEventPublisher eventPublisher,
                                                              TransactionRunner transactionRunner) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(MarkSubscriptionsPastDueByStripeIdCommand command) {
        return transactionRunner.runReturning(() -> {
            List<Subscription> subscriptions =
                    subscriptionRepository.findAllByStripeSubscriptionId(command.stripeSubscriptionId());

            if (subscriptions.isEmpty()) {
                log.warn("Past-due event for unknown stripe subscription {}", command.stripeSubscriptionId());
                return Result.success();
            }

            for (Subscription subscription : subscriptions) {
                Result<Subscription> pastDue = subscription.markPastDue();
                if (pastDue.isFailure()) {
                    log.warn("Cannot mark subscription {} as past due: {}",
                            subscription.getId(), pastDue.getError());
                    continue;
                }
                subscriptionRepository.save(pastDue.getValue());
                eventPublisher.publishAll(pastDue.getValue().getDomainEvents());
                pastDue.getValue().clearDomainEvents();
            }

            return Result.success();
        });
    }
}
