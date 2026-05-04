package com.cyna.modules.order.application.api;

import com.cyna.modules.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
class OrderQueryApiImpl implements OrderQueryApi {

    private final OrderRepository orderRepository;

    OrderQueryApiImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
