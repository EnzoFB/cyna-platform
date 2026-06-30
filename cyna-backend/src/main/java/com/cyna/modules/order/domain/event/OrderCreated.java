package com.cyna.modules.order.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code subtotalHt} is the HT subtotal of the order at the moment it was
 * created. The TTC and VAT amounts of the matching Stripe invoice are the
 * authoritative billed values and are not part of this event.
 */
public record OrderCreated(
        UUID orderId,
        UUID userId,
        BigDecimal subtotalHt,
        Instant occurredAt
) implements DomainEvent {
}
