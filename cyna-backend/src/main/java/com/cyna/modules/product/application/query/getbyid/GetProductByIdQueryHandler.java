package com.cyna.modules.product.application.query.getbyid;

import com.cyna.modules.product.application.promotion.PromotionPriceView;
import com.cyna.modules.product.application.promotion.PromotionPricingResolver;
import com.cyna.modules.product.application.translation.ProductTranslationDto;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.time.Instant;

@Component
public class GetProductByIdQueryHandler implements QueryHandler<GetProductByIdQuery, ProductReadModel> {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionPricingResolver promotionPricingResolver;

    public GetProductByIdQueryHandler(ProductRepository productRepository,
                                      CategoryRepository categoryRepository,
                                      ProductImageRepository productImageRepository,
                                      PromotionRepository promotionRepository,
                                      PromotionPricingResolver promotionPricingResolver) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.promotionRepository = promotionRepository;
        this.promotionPricingResolver = promotionPricingResolver;
    }

    @Override
    public ProductReadModel handle(GetProductByIdQuery query) {
        return productRepository.findById(query.id()).flatMap(product -> {
            var category = categoryRepository.findById(product.getCategoryId()).orElse(null);
            if (category == null || !category.isActive()) {
                return java.util.Optional.empty();
            }
            String categoryName = category.getName();
            var images = productImageRepository.findByProductId(product.getId()).stream()
                    .map(img -> new ProductImageReadModel(
                            img.getId(),
                            Base64.getEncoder().encodeToString(img.getImageData())
                    ))
                    .toList();
            var activePromotion = promotionRepository.findActiveByProductIds(
                    java.util.List.of(product.getId()),
                    Instant.now()
            ).stream().findFirst().orElse(null);
            PromotionPriceView pricing = promotionPricingResolver.resolve(product, activePromotion);

            return java.util.Optional.of(new ProductReadModel(
                    product.getId(),
                    ProductTranslationDto.fromDomainMap(product.getTranslations()),
                    product.getCategoryId(),
                    categoryName,
                    product.getPriorityLevel(),
                    pricing.baseMonthlyPrice(),
                    pricing.baseAnnualPrice(),
                    pricing.discountedMonthlyPrice(),
                    pricing.discountedAnnualPrice(),
                    pricing.discountPercent(),
                    pricing.promotionStartAt(),
                    pricing.promotionEndAt(),
                    product.getCurrency(),
                    product.isPublished(),
                    product.isAvailable(),
                    product.getFreeTrialDays(),
                    images,
                    product.getCreatedAt(),
                    product.getUpdatedAt()
            ));
        }).orElse(null);
    }
}
