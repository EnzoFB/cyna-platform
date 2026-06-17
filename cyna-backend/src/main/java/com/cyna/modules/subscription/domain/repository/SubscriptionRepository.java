package com.cyna.modules.subscription.domain.repository;

import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.shared.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {
    void save(Subscription subscription);
    Optional<Subscription> findById(UUID id);
    Page<Subscription> findAllByUserId(UUID userId, int page, int size, SubscriptionSort sort);
    /** All of the user's subscriptions (unpaged) — RGPD Art. 15/20 export. */
    List<Subscription> findByUserId(UUID userId);
    /** The user's subscriptions for one order — used to resolve an order's Stripe invoices (VAT/TTC). */
    List<Subscription> findByUserIdAndOrderId(UUID userId, UUID orderId);
    /** Whether the user has ever held any subscription for the product (free-trial eligibility). */
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
    List<Subscription> findAllByStripeSubscriptionId(String stripeSubscriptionId);
    List<Subscription> findActiveAutoRenewDueForNotice(Instant fromInclusive, Instant toExclusive);
}
