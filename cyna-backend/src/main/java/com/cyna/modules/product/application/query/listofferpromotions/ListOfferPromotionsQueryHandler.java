package com.cyna.modules.product.application.query.listofferpromotions;

import com.cyna.modules.product.domain.model.CarouselSlot;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.CarouselSlotRepository;
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
    private final CarouselSlotRepository carouselSlotRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;

    public ListOfferPromotionsQueryHandler(PromotionRepository promotionRepository,
                                           CarouselSlotRepository carouselSlotRepository,
                                           ProductRepository productRepository,
                                           ProductImageRepository productImageRepository,
                                           CategoryRepository categoryRepository) {
        this.promotionRepository = promotionRepository;
        this.carouselSlotRepository = carouselSlotRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<OfferPromotionReadModel> handle(ListOfferPromotionsQuery query) {
        Instant now = Instant.now();
        boolean english = query.language() != null && query.language().toLowerCase(Locale.ROOT).startsWith("en");

        Map<UUID, Integer> slotOrderByPromotionId = carouselSlotRepository.findAll().stream()
                .collect(Collectors.toMap(CarouselSlot::promotionId, CarouselSlot::slotOrder));

        if (slotOrderByPromotionId.isEmpty()) {
            return List.of();
        }

        List<Promotion> carouselPromotions = promotionRepository.findAll().stream()
                .filter(p -> slotOrderByPromotionId.containsKey(p.getId()) && p.isActiveAt(now))
                .toList();

        if (carouselPromotions.isEmpty()) {
            return List.of();
        }

        List<UUID> productIds = carouselPromotions.stream()
                .map(Promotion::getProductId)
                .distinct()
                .toList();

        Map<UUID, Product> productsById = productRepository.findAllByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<Category> allCategories = categoryRepository.findAll();
        Map<UUID, String> categoryNames = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        java.util.Set<UUID> activeCategoryIds = allCategories.stream()
                .filter(Category::isActive)
                .map(Category::getId)
                .collect(java.util.stream.Collectors.toSet());

        Map<UUID, String> firstImageByProductId = productImageRepository.findByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getProductId(),
                        image -> Base64.getEncoder().encodeToString(image.getImageData()),
                        (existing, ignored) -> existing
                ));

        return carouselPromotions.stream()
                .map(promotion -> toReadModel(
                        promotion,
                        productsById.get(promotion.getProductId()),
                        categoryNames,
                        activeCategoryIds,
                        firstImageByProductId,
                        slotOrderByPromotionId.get(promotion.getId()),
                        english
                ))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator
                        .comparingInt(OfferPromotionReadModel::carouselOrder)
                        .thenComparing(Comparator.comparingInt(OfferPromotionReadModel::productPriority).reversed())
                        .thenComparing(OfferPromotionReadModel::productName))
                .toList();
    }

    private OfferPromotionReadModel toReadModel(Promotion promotion,
                                                Product product,
                                                Map<UUID, String> categoryNames,
                                                java.util.Set<UUID> activeCategoryIds,
                                                Map<UUID, String> firstImageByProductId,
                                                int slotOrder,
                                                boolean english) {
        if (product == null || !product.isPublished() || !product.isAvailable()
                || !activeCategoryIds.contains(product.getCategoryId())) {
            return null;
        }

        return new OfferPromotionReadModel(
                promotion.getId(),
                product.getId(),
                product.getName(),
                categoryNames.getOrDefault(product.getCategoryId(), "Unknown"),
                promotion.getMarketingText(english ? "en" : "fr"),
                promotion.getDiscountPercent(),
                product.getMonthlyPrice(),
                promotion.applyDiscount(product.getMonthlyPrice()),
                product.getAnnualPrice(),
                promotion.applyDiscount(product.getAnnualPrice()),
                product.getCurrency(),
                firstImageByProductId.get(product.getId()),
                slotOrder,
                product.getPriorityLevel()
        );
    }
}
