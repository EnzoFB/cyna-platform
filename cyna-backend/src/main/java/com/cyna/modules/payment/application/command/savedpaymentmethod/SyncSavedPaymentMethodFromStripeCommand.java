package com.cyna.modules.payment.application.command.savedpaymentmethod;

import com.cyna.shared.application.Command;

/**
 * Reflects a Stripe {@code payment_method.*} webhook into our local
 * {@code saved_payment_methods} cache. Called by the webhook dispatcher.
 *
 * <p>This is what keeps "Mes moyens de paiement" in sync with what the user
 * does in the Stripe Customer Portal:
 *
 * <ul>
 *     <li>{@code payment_method.attached} → create the local row if absent
 *         (user added a card via the portal directly)</li>
 *     <li>{@code payment_method.detached} → remove the local row (user deleted
 *         a card via the portal, or a subscription cancellation triggered
 *         Stripe to detach)</li>
 *     <li>{@code payment_method.automatically_updated} → refresh brand / last4
 *         / expiry on the local row (issuer reissued the card and Stripe's
 *         Card Account Updater picked it up)</li>
 * </ul>
 */
public record SyncSavedPaymentMethodFromStripeCommand(
        String eventType,
        String stripeCustomerId,
        String stripePaymentMethodId
) implements Command<Void> {}
