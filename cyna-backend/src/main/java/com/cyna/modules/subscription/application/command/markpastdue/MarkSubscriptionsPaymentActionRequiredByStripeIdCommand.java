package com.cyna.modules.subscription.application.command.markpastdue;

import com.cyna.shared.application.Command;

/**
 * Triggered by the {@code invoice.payment_action_required} Stripe webhook —
 * renewal off-session charge failed because the customer's bank requires SCA.
 * Marks every local subscription bound to {@code stripeSubscriptionId} as
 * PAST_DUE and raises {@code SubscriptionPaymentActionRequired} carrying the
 * Stripe-hosted invoice URL the customer needs to visit to complete 3DS.
 */
public record MarkSubscriptionsPaymentActionRequiredByStripeIdCommand(
        String stripeSubscriptionId,
        String hostedInvoiceUrl
) implements Command<Void> {}
