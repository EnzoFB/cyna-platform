package com.cyna.modules.subscription.application.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only cross-module access to a user's subscriptions. Currently used by
 * the user module's RGPD Art. 15/20 personal-data export.
 */
public interface SubscriptionQueryApi {

    List<SubscriptionExportView> exportForUser(UUID userId);

    /**
     * The Stripe subscription ids backing one of the user's orders. Each order
     * line becomes its own Stripe Subscription (and thus its own invoice), so the
     * payment module uses these ids to fetch the authoritative VAT/TTC from the
     * matching Stripe invoices for the order confirmation page and email. Scoped
     * by {@code userId} so a caller can never resolve another user's order.
     * Skips lines whose Stripe subscription id is not yet set.
     */
    List<String> findStripeSubscriptionIdsForOrder(UUID userId, UUID orderId);

    /**
     * Minimal projection the notification module uses to label subscription
     * lifecycle emails (cancellation, payment-failed). The subscription domain
     * events carry only ids, so the product name is resolved here.
     */
    Optional<SubscriptionNotificationView> findForNotification(UUID subscriptionId);

    record SubscriptionNotificationView(
            UUID subscriptionId,
            UUID userId,
            String productName
    ) {}

    // ── Reporting (subscription_schema only) — consumed by the dashboard ──────

    /** Number of subscriptions ACTIVE at the given instant (start ≤ t < end). */
    long countActiveSubscriptionsAt(Instant atInclusive);

    /** Distinct calendar years in which subscriptions were created. */
    List<Integer> findSubscriptionYears();

    record SubscriptionExportView(
            UUID subscriptionId,
            String productName,
            String productCategory,
            String billingCycle,
            String status,
            int quantity,
            BigDecimal unitPrice,
            String currency,
            Instant startAt,
            Instant endAt,
            Instant nextBillingAt,
            Instant cancelledAt,
            boolean autoRenew
    ) {}
}
