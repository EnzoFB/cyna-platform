package com.cyna.modules.order.application.query.getbyid;

import com.cyna.shared.application.Query;

import java.util.UUID;

public record GetOrderByIdQuery(UUID orderId) implements Query<OrderReadModel> {
}
