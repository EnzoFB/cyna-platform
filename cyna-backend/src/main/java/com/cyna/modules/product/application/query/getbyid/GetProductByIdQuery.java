package com.cyna.modules.product.application.query.getbyid;

import com.cyna.shared.application.Query;

import java.util.UUID;

public record GetProductByIdQuery(
        UUID id
) implements Query<ProductReadModel> {}
