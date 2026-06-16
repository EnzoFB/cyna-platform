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
 */
public record OrderConfirmationMail(
        String email,
        String firstName,
        String orderReference,
        Instant placedAt,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
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
