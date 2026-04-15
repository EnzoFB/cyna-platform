package com.cyna.modules.product.application.query.list;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.shared.application.Query;
import com.cyna.shared.domain.Page;

import java.util.UUID;

public record ListProductsQuery(
        int page,
        int size,
        Boolean published,
        UUID categoryId,
        String search,
        ProductSort sort
) implements Query<Page<ProductReadModel>> {}
