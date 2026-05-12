package com.cyna.modules.subscription.application.command.cancel;

import com.cyna.modules.payment.application.api.PaymentCommandApi;
import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.model.SubscriptionStatus;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

/**
 * User-initiated cancellation. Single-write pattern: we ask Stripe (the source of
 * truth) to schedule the cancellation, then return the user's projected new state
 * for an instant UI response. The actual DB row is reconciled by the
 * {@code customer.subscription.updated} webhook a few hundred milliseconds later
 * — that's the only path that mutates {@code subscription_schema.subscriptions}
 * for user-initiated cancellations.
 *
 * <p>Why no local write here? Because Stripe is the authoritative system; writing
 * locally before the webhook risks divergence if the webhook also writes (it
 * would have to be idempotent, and dual-write semantics get hairy as soon as
 * cancellations can originate from the Stripe dashboard or customer portal too).
 * Single-write keeps the model honest: every state change in our DB is the echo
 * of a Stripe event.
 */
@Component
public class CancelSubscriptionCommandHandler implements CommandHandler<CancelSubscriptionCommand, SubscriptionReadModel> {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentCommandApi paymentCommandApi;

    public CancelSubscriptionCommandHandler(SubscriptionRepository subscriptionRepository,
                                            PaymentCommandApi paymentCommandApi) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentCommandApi = paymentCommandApi;
    }

    @Override
    public Result<SubscriptionReadModel> handle(CancelSubscriptionCommand command) {
        Subscription subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElse(null);
        if (subscription == null) {
            return Result.failure("Subscription not found: " + command.subscriptionId());
        }
        if (!subscription.getUserId().equals(command.userId())) {
            return Result.failure("Access denied");
        }
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED
                || subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            // Idempotent — already terminal, just echo back current state.
            return Result.success(SubscriptionReadModel.from(subscription));
        }

        // Single-write: only Stripe. The webhook will sync our DB.
        if (subscription.getStripeSubscriptionId() != null) {
            Result<Void> stripeResult = paymentCommandApi
                    .setStripeSubscriptionCancelAtPeriodEnd(subscription.getStripeSubscriptionId(), true);
            if (stripeResult.isFailure()) {
                return Result.failure(stripeResult.getError());
            }
        }

        // Project the state Stripe will replay to us, so the front gets instant
        // feedback without waiting for the webhook (~500ms RTT). The projection
        // is in-memory only — never persisted from this code path.
        Result<Subscription> projected = subscription.cancelAtPeriodEnd();
        return projected.map(SubscriptionReadModel::from);
    }
}
