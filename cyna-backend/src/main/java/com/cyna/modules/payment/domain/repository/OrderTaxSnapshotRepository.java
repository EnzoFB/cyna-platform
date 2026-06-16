package com.cyna.modules.payment.domain.repository;

import com.cyna.modules.payment.domain.model.OrderTaxSnapshot;

import java.util.Optional;
import java.util.UUID;

public interface OrderTaxSnapshotRepository {

    /**
     * The order's captured VAT/TTC snapshot, scoped to its owner. Empty for an
     * order paid before this feature existed, or one provisioned only through
     * the webhook-reconcile path — callers then fall back to the live Stripe
     * read / HT subtotal.
     */
    Optional<OrderTaxSnapshot> findByOrderIdAndUserId(UUID orderId, UUID userId);

    /**
     * Persists the snapshot. Idempotent: the first capture wins (the checkout
     * invoice is immutable), so a re-run of finalize is a no-op.
     */
    void save(OrderTaxSnapshot snapshot);
}
