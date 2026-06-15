package com.cyna.modules.order.application.query.list;

import com.cyna.modules.order.application.query.getbyid.OrderLineReadModel;
import com.cyna.modules.order.application.query.getbyid.OrderReadModel;
import com.cyna.modules.order.domain.repository.OrderRepository;
import com.cyna.modules.order.domain.repository.OrderSort;
import com.cyna.shared.application.QueryHandler;
import com.cyna.shared.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ListOrdersQueryHandler implements QueryHandler<ListOrdersQuery, Page<OrderReadModel>> {

    private final OrderRepository orderRepository;

    public ListOrdersQueryHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Page<OrderReadModel> handle(ListOrdersQuery query) {
        int safePage = Math.max(0, query.page());
        int safeSize = Math.min(Math.max(1, query.size()), 100);

        Page<com.cyna.modules.order.domain.model.Order> page = orderRepository.findAllByUserId(
                query.userId(),
                safePage,
                safeSize,
                OrderSort.parseOrDefault(query.sort())
        );

        var items = page.items().stream()
                .map(order -> new OrderReadModel(
                        order.getId(),
                        order.getUserId(),
                        order.getStatus().name(),
                        order.getSubtotal().amount(),
                        order.getVatAmount().amount(),
                        order.getTotalTtc().amount(),
                        order.getTotalTtc().currency(),
                        order.getBillingAddress(),
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
                                .toList()
                ))
                .toList();

        return new Page<>(items, page.pageNumber(), page.pageSize(), page.totalElements(), page.totalPages());
    }
}
