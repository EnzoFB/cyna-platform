package com.cyna.modules.order.infrastructure.persistence.mapper;

import com.cyna.modules.order.domain.model.Order;
import com.cyna.modules.order.domain.model.OrderLine;
import com.cyna.modules.order.domain.model.OrderStatus;
import com.cyna.modules.order.infrastructure.persistence.entity.OrderJpaEntity;
import com.cyna.modules.order.infrastructure.persistence.entity.OrderLineJpaEntity;
import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.shared.domain.Money;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderJpaMapper {

    public OrderJpaEntity toJpa(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getId());
        entity.setUserId(order.getUserId());
        entity.setStatus(order.getStatus().name());
        entity.setSubtotalAmount(order.getSubtotal().amount());
        entity.setVatAmount(order.getVatAmount().amount());
        entity.setTotalAmount(order.getTotalTtc().amount());
        entity.setCurrency(order.getTotalTtc().currency());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());

        List<OrderLineJpaEntity> lineEntities = new ArrayList<>();
        for (OrderLine line : order.getLines()) {
            OrderLineJpaEntity lineEntity = toJpaLine(line, entity);
            lineEntities.add(lineEntity);
        }
        entity.setLines(lineEntities);
        return entity;
    }

    public Order toDomain(OrderJpaEntity entity) {
        List<OrderLine> lines = entity.getLines().stream()
                .map(this::toDomainLine)
                .toList();

        return Order.reconstitute(
                entity.getId(),
                entity.getUserId(),
                OrderStatus.valueOf(entity.getStatus()),
                lines,
                Money.of(entity.getSubtotalAmount(), entity.getCurrency()),
                Money.of(entity.getVatAmount(), entity.getCurrency()),
                Money.of(entity.getTotalAmount(), entity.getCurrency()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OrderLineJpaEntity toJpaLine(OrderLine line, OrderJpaEntity orderEntity) {
        OrderLineJpaEntity entity = new OrderLineJpaEntity();
        entity.setId(line.getId());
        entity.setOrder(orderEntity);
        entity.setProductId(line.getProductId());
        entity.setProductName(line.getProductName());
        entity.setProductCategory(line.getProductCategory());
        entity.setBillingCycle(line.getBillingCycle().name());
        entity.setQuantity(line.getQuantity());
        entity.setUnitPrice(line.getUnitPrice().amount());
        entity.setCurrency(line.getUnitPrice().currency());
        entity.setCreatedAt(orderEntity.getCreatedAt());
        entity.setUpdatedAt(orderEntity.getUpdatedAt());
        return entity;
    }

    private OrderLine toDomainLine(OrderLineJpaEntity entity) {
        return OrderLine.reconstitute(
                entity.getId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getProductCategory(),
                BillingCycle.valueOf(entity.getBillingCycle()),
                entity.getQuantity(),
                Money.of(entity.getUnitPrice(), entity.getCurrency())
        );
    }
}
