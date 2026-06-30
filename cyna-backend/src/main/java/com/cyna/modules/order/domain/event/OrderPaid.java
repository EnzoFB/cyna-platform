package com.cyna.modules.order.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code subtotalHt} is the HT subtotal stored on the order. The actual TTC
 * charged by Stripe (including the destination VAT or B2B reverse charge)
 * lives on the corresponding Stripe invoice — consumers that need it look it
 * up via the invoice API, they do not get it from this event.
 */
public record OrderPaid(
        UUID orderId,
        UUID userId,
        BigDecimal subtotalHt,
        String lang,
        Instant occurredAt
) implements DomainEvent {}
