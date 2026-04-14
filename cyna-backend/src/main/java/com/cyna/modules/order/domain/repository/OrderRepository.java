package com.cyna.modules.order.domain.repository;

import com.cyna.modules.order.application.query.list.OrderSort;
import com.cyna.modules.order.domain.model.Order;
import com.cyna.shared.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(UUID id);
    Page<Order> findAllByUserId(UUID userId, int page, int size, OrderSort sort);
}
