package com.cyna.modules.product.application.query.list;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.shared.application.Query;
import com.cyna.shared.domain.Page;

public record ListProductsQuery(
        int page,
        int size,
        String status,
        String category,
        String search,
        String sort
) implements Query<Page<ProductReadModel>> {}
