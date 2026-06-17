package com.cyna.modules.notification.application.mail;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Payload for the post-payment order confirmation email. Built by the
 * notification module from data it reads through {@code OrderQueryApi} /
 * {@code UserQueryApi}, then handed to the {@link com.cyna.modules.notification.application.NotificationDispatcher}
 * for rendering and delivery — so neither the handler nor the producing modules
 * depend on Brevo or Thymeleaf.
 *
 * <p>The HT subtotal is always present. {@code vatAmount} / {@code totalTtc}
 * are the authoritative figures read from the order's Stripe invoices (Stripe
 * Tax computes them at subscription creation, including B2B reverse charge);
 * they are {@code null} when the invoice isn't available yet or Stripe couldn't
 * be reached, in which case the email shows the HT subtotal only and points the
 * recipient at the Stripe-hosted invoice for the billed total.
 */
public record OrderConfirmationMail(
        String email,
        String firstName,
        String orderReference,
        Instant placedAt,
        List<Line> lines,
        BigDecimal subtotalHt,
        BigDecimal vatAmount,
        BigDecimal totalTtc,
        boolean reverseCharge,
        String currency,
        String lang
) {
    public record Line(
            String productName,
            String billingCycle,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            int freeTrialDays
    ) {}
}
