package com.cyna.modules.payment.domain.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentGatewayPort {

    SubscriptionResult createSubscription(
            UUID orderId,
            String existingStripeCustomerId,
            String userEmail,
            String userFullName,
            List<SubscriptionLineItem> lineItems,
            String currency,
            String billingCycle
    );

    void cancelSubscription(String stripeSubscriptionId);

    /**
     * Creates a Stripe Customer Portal session for the given customer. Returns the
     * one-time signed URL the user can be redirected to in order to manage their
     * payment methods, view invoices and cancel subscriptions on Stripe-hosted UI.
     */
    String createBillingPortalSession(String stripeCustomerId, String returnUrl);

    StripeWebhookEvent parseWebhookEvent(String payload, String sigHeader);

    record SubscriptionLineItem(
            UUID productId,
            String productName,
            BigDecimal unitAmount,
            int quantity
    ) {}

    record SubscriptionResult(
            String stripeCustomerId,
            String subscriptionId,
            String scheduleId,
            String clientSecret,
            String paymentIntentId
    ) {}

    record StripeWebhookEvent(
            String type,
            String paymentIntentId,
            String subscriptionId,
            String customerId,
            String invoiceId,
            String billingReason,
            Instant periodEnd
    ) {}
}
