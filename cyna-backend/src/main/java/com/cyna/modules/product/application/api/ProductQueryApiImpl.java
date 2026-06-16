package com.cyna.modules.product.application.api;

import com.cyna.modules.product.application.promotion.PromotionPricingResolver;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
class ProductQueryApiImpl implements ProductQueryApi {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionPricingResolver promotionPricingResolver;

    ProductQueryApiImpl(ProductRepository productRepository,
                        CategoryRepository categoryRepository,
                        PromotionRepository promotionRepository,
                        PromotionPricingResolver promotionPricingResolver) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.promotionRepository = promotionRepository;
        this.promotionPricingResolver = promotionPricingResolver;
    }

    @Override
    public Optional<ProductInfo> getById(UUID productId) {
        Instant now = Instant.now();
        return productRepository.findById(productId).map(product -> {
            String categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(Category::getName).orElse("Unknown");
            var activePromotion = promotionRepository.findActiveByProductIds(List.of(productId), now)
                    .stream().findFirst().orElse(null);
            var pricing = promotionPricingResolver.resolve(product, activePromotion);
            var frT = product.getTranslations().get("fr");
            return new ProductInfo(
                    product.getId(),
                    product.getName(),
                    frT != null ? frT.serviceDescription() : "",
                    frT != null ? frT.technicalDescription() : "",
                    pricing.effectiveMonthlyPrice(),
                    pricing.effectiveAnnualPrice(),
                    product.getCurrency(),
                    product.isPublished(),
                    product.isAvailable(),
                    product.getCategoryId(),
                    categoryName,
                    product.getPriorityLevel()
            );
        });
    }

    @Override
    public List<ProductInfo> getByIds(List<UUID> productIds) {
        Instant now = Instant.now();
        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<UUID, Promotion> activePromotionsByProductId =
                promotionPricingResolver.selectActiveByProduct(
                        promotionRepository.findActiveByProductIds(productIds, now),
                        now
                );

        return productRepository.findAllByIds(productIds).stream().map(product -> {
            var pricing = promotionPricingResolver.resolve(product, activePromotionsByProductId.get(product.getId()));
            var frT = product.getTranslations().get("fr");
            return new ProductInfo(
                    product.getId(),
                    product.getName(),
                    frT != null ? frT.serviceDescription() : "",
                    frT != null ? frT.technicalDescription() : "",
                    pricing.effectiveMonthlyPrice(),
                    pricing.effectiveAnnualPrice(),
                    product.getCurrency(),
                    product.isPublished(),
                    product.isAvailable(),
                    product.getCategoryId(),
                    categoryNames.getOrDefault(product.getCategoryId(), "Unknown"),
                    product.getPriorityLevel()
            );
        }).toList();
    }

    @Override
    public boolean exists(UUID productId) {
        return productRepository.existsById(productId);
    }
}
