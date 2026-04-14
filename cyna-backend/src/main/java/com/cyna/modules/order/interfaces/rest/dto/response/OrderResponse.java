package com.cyna.modules.order.interfaces.rest.dto.response;

import com.cyna.modules.order.application.query.getbyid.OrderReadModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        String status,
        BigDecimal subtotalAmount,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant updatedAt,
        List<OrderLineResponse> lines
) {
    public static OrderResponse from(OrderReadModel model) {
        return new OrderResponse(
                model.id(),
                model.userId(),
                model.status(),
                model.subtotalAmount(),
                model.vatAmount(),
                model.totalAmount(),
                model.currency(),
                model.createdAt(),
                model.updatedAt(),
                model.lines().stream()
                        .map(line -> new OrderLineResponse(
                                line.id(),
                                line.productId(),
                                line.productName(),
                                line.productCategory(),
                                line.billingCycle(),
                                line.quantity(),
                                line.unitPrice(),
                                line.currency()
                        ))
                        .toList()
        );
    }
}
