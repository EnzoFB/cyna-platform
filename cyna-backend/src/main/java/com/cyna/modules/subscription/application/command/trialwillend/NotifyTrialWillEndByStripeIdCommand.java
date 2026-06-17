package com.cyna.modules.subscription.application.command.trialwillend;

import com.cyna.shared.application.Command;

import java.time.Instant;

/**
 * Triggered by the {@code customer.subscription.trial_will_end} Stripe webhook
 * (~3 days before the trial converts). Raises {@code SubscriptionTrialWillEnd}
 * for every local subscription bound to {@code stripeSubscriptionId} so the
 * notification module can email the customer ahead of the first charge. No state
 * change — the trial is still active.
 */
public record NotifyTrialWillEndByStripeIdCommand(
        String stripeSubscriptionId,
        Instant trialEndAt
) implements Command<Void> {}
