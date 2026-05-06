package com.cyna.modules.product.application.query.list;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.shared.application.Query;
import com.cyna.shared.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ListProductsQuery(
        int page,
        int size,
        Boolean published,
        Boolean available,
        UUID categoryId,
        List<UUID> categoryIds,
        String search,
        BigDecimal monthlyPriceMin,
        BigDecimal monthlyPriceMax,
        BigDecimal annualPriceMin,
        BigDecimal annualPriceMax,
        Integer minFreeTrialDays,
        ProductSort sort
) implements Query<Page<ProductReadModel>> {}
