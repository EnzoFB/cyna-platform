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
 * <p>Only the HT subtotal is included. The authoritative TTC and VAT amounts
 * live on the Stripe invoice (Stripe Tax computes them at subscription
 * creation, including B2B reverse charge); the email points the recipient at
 * the Stripe-hosted invoice for the billed total.
 */
public record OrderConfirmationMail(
        String email,
        String firstName,
        String orderReference,
        Instant placedAt,
        List<Line> lines,
        BigDecimal subtotalHt,
        String currency,
        String lang
) {
    public record Line(
            String productName,
            String billingCycle,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}
}
