package com.cyna.modules.order.application.api;

import com.cyna.modules.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class OrderQueryApiImpl implements OrderQueryApi {

    private final OrderRepository orderRepository;

    OrderQueryApiImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public boolean userHasOrders(UUID userId) {
        return orderRepository.existsByUserId(userId);
    }

    @Override
    public List<OrderExportView> exportOrdersForUser(UUID userId) {
        return orderRepository.findAllByUserId(userId).stream()
                .map(order -> new OrderExportView(
                        order.getId(),
                        order.getStatus().name(),
                        order.getSubtotal().amount(),
                        order.getVatAmount().amount(),
                        order.getTotalTtc().amount(),
                        order.getTotalTtc().currency(),
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
    public Optional<OrderPaymentView> findOrderForPayment(UUID orderId, UUID userId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getUserId().equals(userId))
                .map(order -> new OrderPaymentView(
                        order.getId(),
                        order.getUserId(),
                        order.getStatus().name(),
                        order.getTotalTtc().amount(),
                        order.getTotalTtc().currency(),
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
