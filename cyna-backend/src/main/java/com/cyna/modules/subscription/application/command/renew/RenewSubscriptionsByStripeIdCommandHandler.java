package com.cyna.modules.subscription.application.command.renew;

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
public class RenewSubscriptionsByStripeIdCommandHandler
        implements CommandHandler<RenewSubscriptionsByStripeIdCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(RenewSubscriptionsByStripeIdCommandHandler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public RenewSubscriptionsByStripeIdCommandHandler(SubscriptionRepository subscriptionRepository,
                                                       DomainEventPublisher eventPublisher,
                                                       TransactionRunner transactionRunner) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(RenewSubscriptionsByStripeIdCommand command) {
        return transactionRunner.runReturning(() -> {
            List<Subscription> subscriptions =
                    subscriptionRepository.findAllByStripeSubscriptionId(command.stripeSubscriptionId());

            if (subscriptions.isEmpty()) {
                log.warn("Renewal received for unknown stripe subscription {}", command.stripeSubscriptionId());
                return Result.success();
            }

            for (Subscription subscription : subscriptions) {
                Result<Subscription> renewed = subscription.renew(
                        command.newPeriodEnd(),
                        command.newPeriodEnd()
                );
                if (renewed.isFailure()) {
                    log.warn("Cannot renew subscription {}: {}", subscription.getId(), renewed.getError());
                    continue;
                }
                subscriptionRepository.save(renewed.getValue());
                eventPublisher.publishAll(renewed.getValue().getDomainEvents());
                renewed.getValue().clearDomainEvents();
            }

            return Result.success();
        });
    }
}
