package com.cyna.modules.product.application.promotion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromotionPriceView(
        BigDecimal baseMonthlyPrice,
        BigDecimal baseAnnualPrice,
        BigDecimal discountedMonthlyPrice,
        BigDecimal discountedAnnualPrice,
        Integer discountPercent,
        UUID promotionId,
        Instant promotionStartAt,
        Instant promotionEndAt
) {
    public BigDecimal effectiveMonthlyPrice() {
        return discountedMonthlyPrice != null ? discountedMonthlyPrice : baseMonthlyPrice;
    }

    public BigDecimal effectiveAnnualPrice() {
        return discountedAnnualPrice != null ? discountedAnnualPrice : baseAnnualPrice;
    }
}
