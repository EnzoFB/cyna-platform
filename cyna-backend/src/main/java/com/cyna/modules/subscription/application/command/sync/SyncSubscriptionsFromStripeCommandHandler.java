package com.cyna.modules.subscription.application.command.sync;

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
public class SyncSubscriptionsFromStripeCommandHandler
        implements CommandHandler<SyncSubscriptionsFromStripeCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(SyncSubscriptionsFromStripeCommandHandler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public SyncSubscriptionsFromStripeCommandHandler(SubscriptionRepository subscriptionRepository,
                                                      DomainEventPublisher eventPublisher,
                                                      TransactionRunner transactionRunner) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(SyncSubscriptionsFromStripeCommand command) {
        return transactionRunner.runReturning(() -> {
            List<Subscription> subscriptions =
                    subscriptionRepository.findAllByStripeSubscriptionId(command.stripeSubscriptionId());

            if (subscriptions.isEmpty()) {
                // First-time created event arriving before our local subscriptions exist
                // (we create them on PaymentSucceeded, which fires off `invoice.paid`).
                // Webhook delivery order is not guaranteed — log and move on; the next
                // `customer.subscription.updated` will reconcile once the local rows exist.
                log.info("Sync event for stripe sub {} — no matching local subscription yet, skipping",
                        command.stripeSubscriptionId());
                return Result.success();
            }

            for (Subscription subscription : subscriptions) {
                Result<Subscription> synced = subscription.syncFromStripeState(
                        command.stripeStatus(),
                        command.cancelAtPeriodEnd(),
                        command.currentPeriodEnd(),
                        command.stripeCanceledAt()
                );
                if (synced.isFailure()) {
                    log.warn("Cannot sync subscription {} from Stripe ({}): {}",
                            subscription.getId(), command.stripeSubscriptionId(), synced.getError());
                    continue;
                }
                Subscription updated = synced.getValue();
                if (updated == subscription) {
                    // No-op transition (state already matched Stripe) — skip the write.
                    continue;
                }
                subscriptionRepository.save(updated);
                eventPublisher.publishAll(updated.getDomainEvents());
                updated.clearDomainEvents();
            }

            return Result.success();
        });
    }
}
