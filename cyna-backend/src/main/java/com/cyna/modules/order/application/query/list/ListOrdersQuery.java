package com.cyna.modules.order.application.query.list;

import com.cyna.modules.order.application.query.getbyid.OrderReadModel;
import com.cyna.shared.application.Query;
import com.cyna.shared.domain.Page;

import java.util.UUID;

public record ListOrdersQuery(
        UUID userId,
        int page,
        int size,
        String sort
) implements Query<Page<OrderReadModel>> {
}
