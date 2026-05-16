package com.cyna.modules.order.application.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderQueryApi {
    Optional<OrderPaymentView> findOrderForPayment(UUID orderId, UUID userId);

    /** RGPD Art. 15/20 — the user's orders for the personal-data export. */
    List<OrderExportView> exportOrdersForUser(UUID userId);

    record OrderExportView(
            UUID orderId,
            String status,
            BigDecimal subtotal,
            BigDecimal vat,
            BigDecimal total,
            String currency,
            Instant createdAt,
            List<OrderExportLine> lines
    ) {}

    record OrderExportLine(
            String productName,
            String productCategory,
            String billingCycle,
            int quantity,
            BigDecimal unitPrice
    ) {}

    /**
     * Whether the user has any order. Orders are the legally-retained
     * accounting records (Code de commerce L123-22, 10 years), so this is the
     * authoritative "has a transactional footprint" check the user module uses
     * to decide hard-delete vs anonymize on an RGPD erasure request.
     */
    boolean userHasOrders(UUID userId);
}
