package com.cyna.modules.order.application.query.admin;

import com.cyna.shared.application.Query;
import com.cyna.shared.domain.Page;

public record ListAllOrdersQuery(String status, int page, int size) implements Query<Page<AdminOrderReadModel>> {}
