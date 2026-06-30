package com.cyna.modules.order.interfaces.rest.dto.response;

import com.cyna.modules.order.application.query.admin.AdminOrderReadModel;

import java.math.BigDecimal;

public record AdminOrderListResponse(
    String id,
    String userId,
    String customerEmail,
    String customerFirstName,
    String customerLastName,
    String status,
    BigDecimal subtotalHt,
    String currency,
    long lineCount,
    String createdAt,
    String updatedAt
) {
    public static AdminOrderListResponse from(AdminOrderReadModel m) {
        return new AdminOrderListResponse(
            m.id().toString(),
            m.userId().toString(),
            m.customerEmail(),
            m.customerFirstName(),
            m.customerLastName(),
            m.status(),
            m.subtotalHt(),
            m.currency(),
            m.lineCount(),
            m.createdAt().toString(),
            m.updatedAt().toString()
        );
    }
}
