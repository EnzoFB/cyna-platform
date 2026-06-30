package com.cyna.modules.product.application.query.listpromotions;

import com.cyna.shared.application.Query;

import java.util.List;

public record ListPromotionsQuery() implements Query<List<PromotionReadModel>> {
}
