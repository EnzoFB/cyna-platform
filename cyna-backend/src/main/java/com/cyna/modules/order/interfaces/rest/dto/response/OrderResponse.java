package com.cyna.modules.order.interfaces.rest.dto.response;

import com.cyna.modules.order.application.query.getbyid.OrderReadModel;
import com.cyna.modules.order.domain.model.BillingAddress;

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
        BillingAddressDto billingAddress,
        Instant createdAt,
        Instant updatedAt,
        List<OrderLineResponse> lines
) {
    public record BillingAddressDto(
            String line1,
            String city,
            String zipCode,
            String countryCode
    ) {
        static BillingAddressDto from(BillingAddress ba) {
            if (ba == null) return null;
            return new BillingAddressDto(ba.line1(), ba.city(), ba.zipCode(), ba.countryCode());
        }
    }

    public static OrderResponse from(OrderReadModel model) {
        return new OrderResponse(
                model.id(),
                model.userId(),
                model.status(),
                model.subtotalAmount(),
                model.vatAmount(),
                model.totalAmount(),
                model.currency(),
                BillingAddressDto.from(model.billingAddress()),
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
