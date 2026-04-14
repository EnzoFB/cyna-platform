package com.cyna.modules.subscription.application.command.cancel;

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

    public CancelSubscriptionCommandHandler(SubscriptionRepository subscriptionRepository,
                                            TransactionRunner transactionRunner,
                                            DomainEventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRunner = transactionRunner;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<SubscriptionReadModel> handle(CancelSubscriptionCommand command) {
        return transactionRunner.runReturning(() -> {
            Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                    .orElse(null);
            if (subscription == null) {
                return Result.failure("Subscription not found: " + command.subscriptionId());
            }
            if (!subscription.getUserId().equals(command.userId())) {
                return Result.failure("Access denied");
            }

            Result<Subscription> cancelledResult = subscription.cancelAtPeriodEnd();
            if (cancelledResult.isFailure()) {
                return Result.failure(cancelledResult.getError());
            }

            Subscription cancelled = cancelledResult.getValue();
            subscriptionRepository.save(cancelled);
            eventPublisher.publishAll(cancelled.getDomainEvents());
            cancelled.clearDomainEvents();

            return Result.success(SubscriptionReadModel.from(cancelled));
        });
    }
}
