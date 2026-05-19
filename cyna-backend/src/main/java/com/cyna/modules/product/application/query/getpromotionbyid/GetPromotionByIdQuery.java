package com.cyna.modules.product.application.query.getpromotionbyid;

import com.cyna.modules.product.application.query.listpromotions.PromotionReadModel;
import com.cyna.shared.application.Query;

import java.util.UUID;

public record GetPromotionByIdQuery(UUID id) implements Query<PromotionReadModel> {
}

