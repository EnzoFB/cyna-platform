package com.cyna.modules.product.application.query.getpromotionbyid;

import com.cyna.modules.product.application.query.listpromotions.PromotionReadModel;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class GetPromotionByIdQueryHandler implements QueryHandler<GetPromotionByIdQuery, PromotionReadModel> {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public GetPromotionByIdQueryHandler(PromotionRepository promotionRepository,
                                        ProductRepository productRepository,
                                        CategoryRepository categoryRepository) {
        this.promotionRepository = promotionRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public PromotionReadModel handle(GetPromotionByIdQuery query) {
        Promotion promotion = promotionRepository.findById(query.id()).orElse(null);
        if (promotion == null) {
            return null;
        }

        Product product = productRepository.findById(promotion.getProductId()).orElse(null);
        if (product == null) {
            return null;
        }

        String categoryName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName)
                .orElse("Unknown");

        return new PromotionReadModel(
                promotion.getId(),
                product.getId(),
                product.getName(),
                categoryName,
                product.getMonthlyPrice(),
                product.getAnnualPrice(),
                promotion.applyDiscount(product.getMonthlyPrice()),
                promotion.applyDiscount(product.getAnnualPrice()),
                product.getCurrency(),
                promotion.getDiscountPercent(),
                promotion.getTranslations(),
                promotion.getStartAt(),
                promotion.getEndAt(),
                promotion.isEnabled(),
                promotion.isShowInCarousel(),
                promotion.getCarouselOrder(),
                promotion.isActiveAt(Instant.now()),
                promotion.getCreatedAt(),
                promotion.getUpdatedAt()
        );
    }
}
