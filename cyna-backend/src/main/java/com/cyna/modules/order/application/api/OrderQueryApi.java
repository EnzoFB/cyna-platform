package com.cyna.modules.order.application.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface OrderQueryApi {
    Optional<OrderPaymentView> findOrderForPayment(UUID orderId, UUID userId);

    /**
     * Read-only projection the notification module uses to build the order
     * confirmation email after an {@code OrderPaid} event. Exposes the HT
     * subtotal only — the authoritative TTC and VAT live on the matching
     * Stripe invoice and are referenced from the email as a separate link.
     */
    Optional<OrderConfirmationView> findOrderForConfirmation(UUID orderId);

    record OrderConfirmationView(
            UUID orderId,
            BigDecimal subtotalHt,
            String currency,
            List<OrderConfirmationLine> lines
    ) {}

    record OrderConfirmationLine(
            String productName,
            String billingCycle,
            int quantity,
            BigDecimal unitPrice,
            int freeTrialDays
    ) {}

    /** RGPD Art. 15/20 — the user's orders for the personal-data export. */
    List<OrderExportView> exportOrdersForUser(UUID userId);

    record OrderExportView(
            UUID orderId,
            String status,
            BigDecimal subtotalHt,
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

    /**
     * Sum of HT subtotal of revenue-bearing orders (PAID/FULFILLED) created in
     * the window. This is the locally-tracked HT revenue. The TTC revenue (HT
     * + collected VAT) lives in Stripe — pull it from there for accounting.
     */
    long sumRevenueBetween(Instant fromInclusive, Instant toExclusive);

    /** Sum of line quantities of revenue-bearing orders created in the window. */
    long sumSalesQuantityBetween(Instant fromInclusive, Instant toExclusive);

    /** HT revenue per month (1-12) of revenue-bearing orders for the given fiscal year. */
    List<MonthlyRevenuePoint> findMonthlyRevenueByYear(int year);

    /** Best-selling products of the year, by snapshotted order-line name (no product_schema read). */
    List<TopProductPoint> findTopProductsByYear(int year, int limit);

    /** Order counts grouped by status for the given fiscal year. */
    Map<String, Long> countOrdersByStatusForYear(int year);

    /**
     * HT revenue and sales count of revenue-bearing orders for each of the last
     * {@code days} calendar days up to (and including) today, oldest first.
     */
    List<DailyRevenuePoint> findDailyRevenue(int days);

    /**
     * HT revenue and sales count of revenue-bearing orders for each of the last
     * {@code weeks} ISO weeks up to (and including) the current week, oldest first.
     */
    List<WeeklyRevenuePoint> findWeeklyRevenue(int weeks);

    /**
     * Average cart value per product category over the given fiscal year:
     * SUM(quantity * unit_price) / COUNT(DISTINCT order_id) for revenue-bearing orders.
     */
    List<CategoryAvgCartPoint> findCategoryAverageCartByYear(int year);

    List<CategoryAvgCartPoint> findCategoryAverageCartBetween(Instant fromInclusive, Instant toExclusive);

    /** HT revenue and quantity sold per product category over the given fiscal year. */
    List<CategorySalesPoint> findCategorySalesByYear(int year);

    List<CategorySalesPoint> findCategorySalesBetween(Instant fromInclusive, Instant toExclusive);

    /** Distinct calendar years in which orders were created. */
    List<Integer> findOrderYears();

    record MonthlyRevenuePoint(int month, long revenueAmount) {}

    record TopProductPoint(UUID productId, String name, long salesCount, long revenueAmount) {}

    record DailyRevenuePoint(LocalDate date, long revenueAmount, long salesCount) {}

    record WeeklyRevenuePoint(LocalDate weekStart, long revenueAmount, long salesCount) {}

    record CategoryAvgCartPoint(String category, long avgCartValue, long orderCount) {}

    record CategorySalesPoint(String category, long revenueAmount, long quantity) {}
}
