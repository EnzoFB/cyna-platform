package com.cyna.modules.subscription.application.command.autorenew;

import com.cyna.modules.payment.application.api.PaymentCommandApi;
import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

/**
 * User-initiated auto-renew toggle. Single-write pattern: we push the change to
 * Stripe (the authoritative side, via {@code cancel_at_period_end} which is the
 * inverse of auto-renew) and return the projected state for instant UI feedback.
 * The DB row is reconciled by the {@code customer.subscription.updated} webhook.
 *
 * <p>See {@link com.cyna.modules.subscription.application.command.cancel.CancelSubscriptionCommandHandler}
 * for the rationale behind not writing to the DB here.
 */
@Component
public class UpdateSubscriptionAutoRenewCommandHandler implements CommandHandler<UpdateSubscriptionAutoRenewCommand, SubscriptionReadModel> {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentCommandApi paymentCommandApi;

    public UpdateSubscriptionAutoRenewCommandHandler(SubscriptionRepository subscriptionRepository,
                                                     PaymentCommandApi paymentCommandApi) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentCommandApi = paymentCommandApi;
    }

    @Override
    public Result<SubscriptionReadModel> handle(UpdateSubscriptionAutoRenewCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId()).orElse(null);
        if (subscription == null) {
            return Result.failure("Subscription not found: " + command.subscriptionId());
        }
        if (!subscription.getUserId().equals(command.userId())) {
            return Result.failure("Access denied");
        }

        Result<Subscription> projected = subscription.updateAutoRenew(command.autoRenew());
        if (projected.isFailure()) {
            return Result.failure(projected.getError());
        }
        // No state change (toggling to the same value) → echo back current state.
        if (projected.getValue() == subscription) {
            return Result.success(SubscriptionReadModel.from(subscription));
        }

        // Single-write: only Stripe. cancel_at_period_end is the inverse of auto-renew.
        if (subscription.getStripeSubscriptionId() != null) {
            Result<Void> stripeResult = paymentCommandApi.setStripeSubscriptionCancelAtPeriodEnd(
                    subscription.getStripeSubscriptionId(),
                    !command.autoRenew()
            );
            if (stripeResult.isFailure()) {
                return Result.failure(stripeResult.getError());
            }
        }

        // Return the projected state without persisting — the webhook will sync DB.
        return Result.success(SubscriptionReadModel.from(projected.getValue()));
    }
}
