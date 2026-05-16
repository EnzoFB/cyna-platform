package com.cyna.modules.subscription.application.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only cross-module access to a user's subscriptions. Currently used by
 * the user module's RGPD Art. 15/20 personal-data export.
 */
public interface SubscriptionQueryApi {

    List<SubscriptionExportView> exportForUser(UUID userId);

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
