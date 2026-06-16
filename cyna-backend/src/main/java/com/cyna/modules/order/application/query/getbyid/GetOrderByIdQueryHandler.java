package com.cyna.modules.order.application.query.getbyid;

import com.cyna.modules.order.domain.repository.OrderRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;


@Component
public class GetOrderByIdQueryHandler implements QueryHandler<GetOrderByIdQuery, OrderReadModel> {

    private final OrderRepository orderRepository;

    public GetOrderByIdQueryHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderReadModel handle(GetOrderByIdQuery query) {
        return orderRepository.findById(query.orderId())
                .map(order -> new OrderReadModel(
                        order.getId(),
                        order.getUserId(),
                        order.getStatus().name(),
                        order.getSubtotalHt().amount(),
                        order.getSubtotalHt().currency(),
                        BillingAddressView.from(order.getBillingAddress()),
                        order.getCreatedAt(),
                        order.getUpdatedAt(),
                        order.getLines().stream()
                                .map(line -> new OrderLineReadModel(
                                        line.getId(),
                                        line.getProductId(),
                                        line.getProductName(),
                                        line.getProductCategory(),
                                        line.getBillingCycle().name(),
                                        line.getQuantity(),
                                        line.getUnitPrice().amount(),
                                        line.getUnitPrice().currency()
                                ))
                                .collect(Collectors.toList())
                ))
                .orElse(null);
    }
}
