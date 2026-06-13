package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.listofferpromotions.OfferPromotionReadModel;

import java.math.BigDecimal;
import java.util.UUID;

public record OfferPromotionResponse(
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
        int carouselOrder
) {
    public static OfferPromotionResponse from(OfferPromotionReadModel model) {
        return new OfferPromotionResponse(
                model.promotionId(),
                model.productId(),
                model.productName(),
                model.productCategoryName(),
                model.marketingText(),
                model.discountPercent(),
                model.originalMonthlyPrice(),
                model.promotionalMonthlyPrice(),
                model.originalAnnualPrice(),
                model.promotionalAnnualPrice(),
                model.currency(),
                model.primaryImageBase64(),
                model.carouselOrder()
        );
    }
}
