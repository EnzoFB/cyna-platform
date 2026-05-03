package com.cyna.modules.subscription.application.command.create;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class CreateSubscriptionCommandHandler implements CommandHandler<CreateSubscriptionCommand, SubscriptionReadModel> {

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRunner transactionRunner;
    private final DomainEventPublisher eventPublisher;

    public CreateSubscriptionCommandHandler(SubscriptionRepository subscriptionRepository,
                                            TransactionRunner transactionRunner,
                                            DomainEventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRunner = transactionRunner;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<SubscriptionReadModel> handle(CreateSubscriptionCommand command) {
        return transactionRunner.runReturning(() -> {
            Subscription subscription = Subscription.createActive(
                    command.userId(),
                    command.orderId(),
                    command.productId(),
                    command.productName(),
                    command.productCategory(),
                    command.billingCycle(),
                    command.quantity(),
                    Money.of(command.unitPrice(), command.currency()),
                    command.startAt(),
                    command.endAt(),
                    command.nextBillingAt(),
                    command.stripeSubscriptionId(),
                    command.stripeScheduleId()
            );

            subscriptionRepository.save(subscription);
            eventPublisher.publishAll(subscription.getDomainEvents());
            subscription.clearDomainEvents();

            return Result.success(SubscriptionReadModel.from(subscription));
        });
    }
}
