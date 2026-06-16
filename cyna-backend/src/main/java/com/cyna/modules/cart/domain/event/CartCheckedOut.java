package com.cyna.modules.cart.domain.event;

import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Fact: a user finalised the checkout of an active cart, and the resulting
 * order was created. Published by {@code CheckoutCartCommandHandler} after the
 * cart is marked {@code CHECKED_OUT} and the order is persisted, inside the
 * same transaction. Downstream listeners (analytics, abandoned-cart relance,
 * post-purchase touchpoints) react via {@code @TransactionalEventListener
 * (phase = AFTER_COMMIT)} like every other domain event in the codebase.
 *
 * <p>The {@code orderId} is included so that consumers can correlate the
 * checkout fact with the order it produced without an extra lookup.
 */
public record CartCheckedOut(
        UUID cartId,
        UUID userId,
        UUID orderId,
        List<Line> lines,
        Instant occurredAt
) implements DomainEvent {

    public CartCheckedOut {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public record Line(
            UUID productId,
            String productName,
            String productCategory,
            BillingCycle billingCycle,
            int quantity
    ) {
    }
}
