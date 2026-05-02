package com.cyna.modules.subscription.application.command.autorenew;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class UpdateSubscriptionAutoRenewCommandHandler implements CommandHandler<UpdateSubscriptionAutoRenewCommand, SubscriptionReadModel> {

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRunner transactionRunner;

    public UpdateSubscriptionAutoRenewCommandHandler(SubscriptionRepository subscriptionRepository,
                                                     TransactionRunner transactionRunner) {
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<SubscriptionReadModel> handle(UpdateSubscriptionAutoRenewCommand command) {
        return transactionRunner.runReturning(() -> {
            Subscription subscription = subscriptionRepository.findById(command.subscriptionId()).orElse(null);
            if (subscription == null) {
                return Result.failure("Subscription not found: " + command.subscriptionId());
            }
            if (!subscription.getUserId().equals(command.userId())) {
                return Result.failure("Access denied");
            }

            Result<Subscription> updateResult = subscription.updateAutoRenew(command.autoRenew());
            if (updateResult.isFailure()) {
                return Result.failure(updateResult.getError());
            }

            Subscription updated = updateResult.getValue();
            subscriptionRepository.save(updated);

            return Result.success(SubscriptionReadModel.from(updated));
        });
    }
}
