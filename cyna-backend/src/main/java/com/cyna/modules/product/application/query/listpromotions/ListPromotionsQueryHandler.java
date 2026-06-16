package com.cyna.modules.product.application.query.listpromotions;

import com.cyna.modules.product.application.translation.PromotionTranslationDto;
import com.cyna.modules.product.domain.model.CarouselSlot;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.CarouselSlotRepository;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.shared.application.QueryHandler;
import com.cyna.shared.application.TransactionRunner;
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
    private final CarouselSlotRepository carouselSlotRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRunner transactionRunner;

    public ListPromotionsQueryHandler(PromotionRepository promotionRepository,
                                      CarouselSlotRepository carouselSlotRepository,
                                      ProductRepository productRepository,
                                      CategoryRepository categoryRepository,
                                      TransactionRunner transactionRunner) {
        this.promotionRepository = promotionRepository;
        this.carouselSlotRepository = carouselSlotRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public List<PromotionReadModel> handle(ListPromotionsQuery query) {
        Instant now = Instant.now();
        List<Promotion> promotions = promotionRepository.findAll().stream()
                .sorted(Comparator.comparing(Promotion::getCreatedAt).reversed())
                .toList();

        // Lazy cleanup: remove expired promotions from carousel and compact slot orders.
        // A promotion is expired when enabled=true but endAt is in the past.
        evictExpiredCarouselSlots(promotions, now);

        Map<UUID, Integer> carouselOrderByPromotionId = carouselSlotRepository.findAll().stream()
                .collect(Collectors.toMap(CarouselSlot::promotionId, CarouselSlot::slotOrder));

        Map<UUID, Product> productsById = productRepository.findAllByIds(
                        promotions.stream().map(Promotion::getProductId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return promotions.stream()
                .map(p -> toReadModel(p, productsById.get(p.getProductId()), categoryNames, carouselOrderByPromotionId, now))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Removes carousel slots whose promotion has expired (enabled=true, endAt in the past),
     * then compacts the remaining slot orders so they stay consecutive.
     */
    private void evictExpiredCarouselSlots(List<Promotion> promotions, Instant now) {
        List<CarouselSlot> currentSlots = carouselSlotRepository.findAll();
        if (currentSlots.isEmpty()) return;

        Map<UUID, Promotion> promotionById = promotions.stream()
                .collect(Collectors.toMap(Promotion::getId, Function.identity()));

        List<UUID> expiredIds = currentSlots.stream()
                .map(CarouselSlot::promotionId)
                .filter(id -> {
                    Promotion p = promotionById.get(id);
                    return p != null && p.isEnabled() && p.getEndAt().isBefore(now);
                })
                .toList();

        if (expiredIds.isEmpty()) return;

        transactionRunner.run(() -> {
            expiredIds.forEach(carouselSlotRepository::remove);
            List<UUID> remaining = carouselSlotRepository.findAll().stream()
                    .map(CarouselSlot::promotionId)
                    .toList();
            if (!remaining.isEmpty()) {
                carouselSlotRepository.reorder(remaining);
            }
        });
    }

    private PromotionReadModel toReadModel(Promotion promotion,
                                           Product product,
                                           Map<UUID, String> categoryNames,
                                           Map<UUID, Integer> carouselOrderByPromotionId,
                                           Instant now) {
        if (product == null) {
            return null;
        }

        Integer slotOrder = carouselOrderByPromotionId.get(promotion.getId());

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
                PromotionTranslationDto.fromDomainMap(promotion.getTranslations()),
                promotion.getStartAt(),
                promotion.getEndAt(),
                promotion.isEnabled(),
                slotOrder != null,
                slotOrder,
                promotion.isActiveAt(now),
                product.isAvailable(),
                product.isPublished(),
                promotion.getCreatedAt(),
                promotion.getUpdatedAt()
        );
    }
}
