package com.cyna.modules.order.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when an order is paid. {@code subtotalHt} is the HT subtotal stored
 * on the order; the authoritative TTC/VAT live on the Stripe invoice and are
 * looked up via the payment API, not carried here.
 */
public record OrderPaidIntegrationEvent(
        UUID orderId,
        UUID userId,
        BigDecimal subtotalHt,
        String lang,
        Instant occurredAt
) implements IntegrationEvent {}
