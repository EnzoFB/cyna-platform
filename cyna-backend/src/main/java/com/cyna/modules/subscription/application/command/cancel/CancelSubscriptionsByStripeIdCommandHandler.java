package com.cyna.modules.subscription.application.command.cancel;

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
public class CancelSubscriptionsByStripeIdCommandHandler
        implements CommandHandler<CancelSubscriptionsByStripeIdCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(CancelSubscriptionsByStripeIdCommandHandler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public CancelSubscriptionsByStripeIdCommandHandler(SubscriptionRepository subscriptionRepository,
                                                        DomainEventPublisher eventPublisher,
                                                        TransactionRunner transactionRunner) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(CancelSubscriptionsByStripeIdCommand command) {
        return transactionRunner.runReturning(() -> {
            List<Subscription> subscriptions =
                    subscriptionRepository.findAllByStripeSubscriptionId(command.stripeSubscriptionId());

            if (subscriptions.isEmpty()) {
                log.warn("Cancellation received for unknown stripe subscription {}",
                        command.stripeSubscriptionId());
                return Result.success();
            }

            for (Subscription subscription : subscriptions) {
                Result<Subscription> cancelled = subscription.cancelAtPeriodEnd();
                if (cancelled.isFailure()) {
                    log.warn("Cannot cancel subscription {}: {}",
                            subscription.getId(), cancelled.getError());
                    continue;
                }
                subscriptionRepository.save(cancelled.getValue());
                eventPublisher.publishAll(cancelled.getValue().getDomainEvents());
                cancelled.getValue().clearDomainEvents();
            }

            return Result.success();
        });
    }
}
