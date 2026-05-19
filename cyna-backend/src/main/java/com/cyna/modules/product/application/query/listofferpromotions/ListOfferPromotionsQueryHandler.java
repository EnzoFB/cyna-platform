package com.cyna.modules.product.application.query.listofferpromotions;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ListOfferPromotionsQueryHandler implements QueryHandler<ListOfferPromotionsQuery, List<OfferPromotionReadModel>> {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;

    public ListOfferPromotionsQueryHandler(PromotionRepository promotionRepository,
                                           ProductRepository productRepository,
                                           ProductImageRepository productImageRepository,
                                           CategoryRepository categoryRepository) {
        this.promotionRepository = promotionRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<OfferPromotionReadModel> handle(ListOfferPromotionsQuery query) {
        Instant now = Instant.now();
        boolean english = query.language() != null && query.language().toLowerCase(Locale.ROOT).startsWith("en");

        List<Promotion> activePromotions = promotionRepository.findAll().stream()
                .filter(promotion -> promotion.isActiveAt(now))
                .toList();
        if (activePromotions.isEmpty()) {
            return List.of();
        }

        List<UUID> productIds = activePromotions.stream()
                .map(Promotion::getProductId)
                .distinct()
                .toList();

        Map<UUID, Product> productsById = productRepository.findAllByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        Map<UUID, String> firstImageByProductId = productImageRepository.findByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getProductId(),
                        image -> Base64.getEncoder().encodeToString(image.getImageData()),
                        (existing, ignored) -> existing
                ));

        return activePromotions.stream()
                .map(promotion -> toReadModel(
                        promotion,
                        productsById.get(promotion.getProductId()),
                        categoryNames,
                        firstImageByProductId,
                        english
                ))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator
                        .comparingInt(OfferPromotionReadModel::productPriority).reversed()
                        .thenComparing(OfferPromotionReadModel::productName))
                .toList();
    }

    private OfferPromotionReadModel toReadModel(Promotion promotion,
                                                Product product,
                                                Map<UUID, String> categoryNames,
                                                Map<UUID, String> firstImageByProductId,
                                                boolean english) {
        if (product == null || !product.isPublished() || !product.isAvailable()) {
            return null;
        }

        return new OfferPromotionReadModel(
                promotion.getId(),
                product.getId(),
                product.getName(),
                categoryNames.getOrDefault(product.getCategoryId(), "Unknown"),
                english ? promotion.getMarketingTextEn() : promotion.getMarketingTextFr(),
                promotion.getDiscountPercent(),
                product.getMonthlyPrice(),
                promotion.applyDiscount(product.getMonthlyPrice()),
                product.getAnnualPrice(),
                promotion.applyDiscount(product.getAnnualPrice()),
                product.getCurrency(),
                firstImageByProductId.get(product.getId()),
                product.getPriorityLevel()
        );
    }
}
