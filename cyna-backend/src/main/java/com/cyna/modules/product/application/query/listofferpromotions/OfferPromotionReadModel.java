package com.cyna.modules.product.application.query.listofferpromotions;

import java.math.BigDecimal;
import java.util.UUID;

public record OfferPromotionReadModel(
        UUID promotionId,
        UUID productId,
        String productName,
        String productCategoryName,
        String marketingText,
        int discountPercent,
        BigDecimal originalMonthlyPrice,
        BigDecimal promotionalMonthlyPrice,
        BigDecimal originalAnnualPrice,
        BigDecimal promotionalAnnualPrice,
        String currency,
        String primaryImageBase64,
        int productPriority
) {
}
