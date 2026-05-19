package com.cyna.modules.product.application.query.listpromotions;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ListPromotionsQueryHandler implements QueryHandler<ListPromotionsQuery, List<PromotionReadModel>> {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ListPromotionsQueryHandler(PromotionRepository promotionRepository,
                                      ProductRepository productRepository,
                                      CategoryRepository categoryRepository) {
        this.promotionRepository = promotionRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<PromotionReadModel> handle(ListPromotionsQuery query) {
        Instant now = Instant.now();
        List<Promotion> promotions = promotionRepository.findAll().stream()
                .sorted(Comparator.comparing(Promotion::getCreatedAt).reversed())
                .toList();

        Map<UUID, Product> productsById = productRepository.findAllByIds(
                        promotions.stream().map(Promotion::getProductId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return promotions.stream()
                .map(promotion -> toReadModel(promotion, productsById.get(promotion.getProductId()), categoryNames, now))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private PromotionReadModel toReadModel(Promotion promotion,
                                           Product product,
                                           Map<UUID, String> categoryNames,
                                           Instant now) {
        if (product == null) {
            return null;
        }

        return new PromotionReadModel(
                promotion.getId(),
                product.getId(),
                product.getName(),
                categoryNames.getOrDefault(product.getCategoryId(), "Unknown"),
                product.getMonthlyPrice(),
                product.getAnnualPrice(),
                promotion.applyDiscount(product.getMonthlyPrice()),
                promotion.applyDiscount(product.getAnnualPrice()),
                product.getCurrency(),
                promotion.getDiscountPercent(),
                promotion.getMarketingTextFr(),
                promotion.getMarketingTextEn(),
                promotion.getStartAt(),
                promotion.getEndAt(),
                promotion.isEnabled(),
                promotion.isActiveAt(now),
                promotion.getCreatedAt(),
                promotion.getUpdatedAt()
        );
    }
}

