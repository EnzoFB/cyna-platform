package com.cyna.modules.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Authoritative VAT/TTC of a paid order, captured once at payment time from the
 * Stripe checkout invoices (Stripe Tax computes the destination VAT and the
 * intra-EU B2B reverse charge). The order itself stores only the HT subtotal
 * (see migration V3); this is a <b>read cache</b> of Stripe's authoritative
 * figures so the confirmation page and email serve the billed total without
 * calling Stripe live on every view. The Stripe invoice remains the legal
 * source of record.
 *
 * <p>Scoped by {@code userId} so a snapshot can never be read for another
 * user's order (same protection as the live read path).
 */
public record OrderTaxSnapshot(
        UUID orderId,
        UUID userId,
        BigDecimal subtotalHt,
        BigDecimal vatAmount,
        BigDecimal totalTtc,
        String currency,
        boolean reverseCharge,
        Instant capturedAt
) {}
