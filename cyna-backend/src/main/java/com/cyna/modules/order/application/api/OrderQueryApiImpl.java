package com.cyna.modules.order.application.api;

import com.cyna.modules.order.application.query.reporting.OrderReportingPort;
import com.cyna.modules.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
class OrderQueryApiImpl implements OrderQueryApi {

    private final OrderRepository orderRepository;
    private final OrderReportingPort reportingPort;

    OrderQueryApiImpl(OrderRepository orderRepository, OrderReportingPort reportingPort) {
        this.orderRepository = orderRepository;
        this.reportingPort = reportingPort;
    }

    @Override
    public boolean userHasOrders(UUID userId) {
        return orderRepository.existsByUserId(userId);
    }

    @Override
    public long sumRevenueBetween(Instant fromInclusive, Instant toExclusive) {
        return reportingPort.sumRevenueBetween(fromInclusive, toExclusive);
    }

    @Override
    public long sumSalesQuantityBetween(Instant fromInclusive, Instant toExclusive) {
        return reportingPort.sumSalesQuantityBetween(fromInclusive, toExclusive);
    }

    @Override
    public List<MonthlyRevenuePoint> findMonthlyRevenueByYear(int year) {
        return reportingPort.findMonthlyRevenueByYear(year);
    }

    @Override
    public List<TopProductPoint> findTopProductsByYear(int year, int limit) {
        return reportingPort.findTopProductsByYear(year, limit);
    }

    @Override
    public Map<String, Long> countOrdersByStatusForYear(int year) {
        return reportingPort.countOrdersByStatusForYear(year);
    }

    @Override
    public List<Integer> findOrderYears() {
        return reportingPort.findOrderYears();
    }

    @Override
    public List<OrderExportView> exportOrdersForUser(UUID userId) {
        return orderRepository.findAllByUserId(userId).stream()
                .map(order -> new OrderExportView(
                        order.getId(),
                        order.getStatus().name(),
                        order.getSubtotalHt().amount(),
                        order.getSubtotalHt().currency(),
                        order.getCreatedAt(),
                        order.getLines().stream()
                                .map(line -> new OrderExportLine(
                                        line.getProductName(),
                                        line.getProductCategory(),
                                        line.getBillingCycle().name(),
                                        line.getQuantity(),
                                        line.getUnitPrice().amount()
                                ))
                                .toList()
                ))
                .toList();
    }

    @Override
    public Optional<OrderConfirmationView> findOrderForConfirmation(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(order -> new OrderConfirmationView(
                        order.getId(),
                        order.getSubtotalHt().amount(),
                        order.getSubtotalHt().currency(),
                        order.getLines().stream()
                                .map(line -> new OrderConfirmationLine(
                                        line.getProductName(),
                                        line.getBillingCycle().name(),
                                        line.getQuantity(),
                                        line.getUnitPrice().amount()
                                ))
                                .toList()
                ));
    }

    @Override
    public Optional<OrderPaymentView> findOrderForPayment(UUID orderId, UUID userId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getUserId().equals(userId))
                .map(order -> new OrderPaymentView(
                        order.getId(),
                        order.getUserId(),
                        order.getStatus().name(),
                        order.getSubtotalHt().amount(),
                        order.getSubtotalHt().currency(),
                        order.getLines().stream()
                                .map(line -> new OrderPaymentView.OrderLineView(
                                        line.getId(),
                                        line.getProductId(),
                                        line.getProductName(),
                                        line.getProductCategory(),
                                        line.getBillingCycle().name(),
                                        line.getQuantity(),
                                        line.getUnitPrice().amount()
                                ))
                                .toList()
                ));
    }
}
