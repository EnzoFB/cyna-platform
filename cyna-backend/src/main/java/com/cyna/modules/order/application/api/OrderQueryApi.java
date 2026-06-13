package com.cyna.modules.order.application.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

    // ── Reporting (order_schema only) — consumed by the dashboard module ──────

    /** Sum of total (TTC) of revenue-bearing orders (PAID/FULFILLED) created in the window. */
    long sumRevenueBetween(Instant fromInclusive, Instant toExclusive);

    /** Sum of line quantities of revenue-bearing orders created in the window. */
    long sumSalesQuantityBetween(Instant fromInclusive, Instant toExclusive);

    /** Revenue per month (1-12) of revenue-bearing orders for the given fiscal year. */
    List<MonthlyRevenuePoint> findMonthlyRevenueByYear(int year);

    /** Best-selling products of the year, by snapshotted order-line name (no product_schema read). */
    List<TopProductPoint> findTopProductsByYear(int year, int limit);

    /** Order counts grouped by status for the given fiscal year. */
    Map<String, Long> countOrdersByStatusForYear(int year);

    /** Distinct calendar years in which orders were created. */
    List<Integer> findOrderYears();

    record MonthlyRevenuePoint(int month, long revenueAmount) {}

    record TopProductPoint(UUID productId, String name, long salesCount, long revenueAmount) {}
}
