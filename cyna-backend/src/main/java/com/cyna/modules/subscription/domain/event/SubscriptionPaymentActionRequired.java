package com.cyna.modules.subscription.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a renewal off-session charge specifically failed because the
 * customer's bank requires SCA (PSD2 strong customer authentication). The
 * subscription is in PAST_DUE — Stripe will retry, but every retry fails the
 * same way until the customer completes the 3DS challenge.
 *
 * <p>This is the renewal-time counterpart to the {@code requires_action} case
 * we handle at first checkout. The notification module reacts by emailing the
 * customer with {@code hostedInvoiceUrl} — a Stripe-signed URL where they can
 * complete the SCA flow on Stripe-hosted UI, without us implementing a custom
 * 3DS resume page.
 *
 * <p>Distinct from {@link SubscriptionPaymentFailed}: that one fires on
 * generic decline (insufficient funds, expired card, …) where the user must
 * update their payment method; this one fires when the existing payment
 * method only needs re-authentication.
 */
public record SubscriptionPaymentActionRequired(
        UUID subscriptionId,
        UUID userId,
        UUID orderId,
        UUID productId,
        String hostedInvoiceUrl,
        Instant occurredAt
) implements DomainEvent {
}
