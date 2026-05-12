package com.cyna.shared.application.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Payload for the post-payment order confirmation email. Carried across the
 * application/infrastructure boundary so command/event handlers can build the
 * data without depending on Brevo or Thymeleaf.
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
