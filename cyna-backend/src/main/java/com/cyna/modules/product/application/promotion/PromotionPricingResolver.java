package com.cyna.modules.product.application.promotion;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.Promotion;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PromotionPricingResolver {

    public PromotionPriceView resolve(Product product, Promotion activePromotion) {
        if (activePromotion == null) {
            return new PromotionPriceView(
                    product.getMonthlyPrice(),
                    product.getAnnualPrice(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        return new PromotionPriceView(
                product.getMonthlyPrice(),
                product.getAnnualPrice(),
                activePromotion.applyDiscount(product.getMonthlyPrice()),
                activePromotion.applyDiscount(product.getAnnualPrice()),
                activePromotion.getDiscountPercent(),
                activePromotion.getId(),
                activePromotion.getStartAt(),
                activePromotion.getEndAt()
        );
    }

    public Map<UUID, Promotion> selectActiveByProduct(List<Promotion> promotions, Instant atInstant) {
        return promotions.stream()
                .filter(promotion -> promotion.isActiveAt(atInstant))
                .collect(Collectors.toMap(
                        Promotion::getProductId,
                        Function.identity(),
                        this::pickLatestByStartAt
                ));
    }

    public Map<UUID, Promotion> indexByProduct(List<Promotion> promotions) {
        return promotions.stream()
                .collect(Collectors.toMap(
                        Promotion::getProductId,
                        Function.identity(),
                        this::pickLatestByStartAt
                ));
    }

    private Promotion pickLatestByStartAt(Promotion first, Promotion second) {
        Comparator<Promotion> comparator = Comparator
                .comparing(Promotion::getStartAt)
                .thenComparing(Promotion::getCreatedAt);
        return comparator.compare(first, second) >= 0 ? first : second;
    }
}

