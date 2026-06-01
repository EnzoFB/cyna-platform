package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.model.PromotionTranslation;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.PromotionJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.PromotionTranslationJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JpaPromotionRepositoryAdapter implements PromotionRepository {

    private final SpringDataPromotionRepository springRepository;
    private final SpringDataProductRepository   productRepository;

    public JpaPromotionRepositoryAdapter(SpringDataPromotionRepository springRepository,
                                         SpringDataProductRepository productRepository) {
        this.springRepository = springRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void save(Promotion promotion) {
        ProductJpaEntity productRef = productRepository.getReferenceById(promotion.getProductId());

        PromotionJpaEntity entity = springRepository.findById(promotion.getId())
                .orElseGet(PromotionJpaEntity::new);

        entity.setId(promotion.getId());
        entity.setProduct(productRef);
        entity.setDiscountPercent(promotion.getDiscountPercent());
        entity.setStartAt(promotion.getStartAt());
        entity.setEndAt(promotion.getEndAt());
        entity.setEnabled(promotion.isEnabled());
        entity.setShowInCarousel(promotion.isShowInCarousel());
        entity.setCarouselOrder(promotion.getCarouselOrder());
        entity.setCreatedAt(promotion.getCreatedAt());
        entity.setUpdatedAt(promotion.getUpdatedAt());

        Set<PromotionTranslationJpaEntity> translations = new HashSet<>();
        promotion.getTranslations().forEach((locale, t) ->
                translations.add(PromotionTranslationJpaEntity.of(entity, locale, t.marketingText()))
        );
        entity.setTranslations(translations);

        springRepository.save(entity);
    }

    @Override
    public Optional<Promotion> findById(UUID id) {
        return springRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Promotion> findAll() {
        return springRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Promotion> findByProductIds(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return springRepository.findAllByProduct_IdIn(productIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Promotion> findActiveByProductIds(Collection<UUID> productIds, Instant atInstant) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return springRepository.findActiveByProductIds(productIds, atInstant).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsEnabledOverlappingWindow(UUID productId,
                                                  Instant startAt,
                                                  Instant endAt,
                                                  UUID excludedPromotionId) {
        return springRepository.existsEnabledOverlappingWindow(productId, startAt, endAt, excludedPromotionId);
    }

    @Override
    public boolean existsCarouselOrder(Integer carouselOrder, UUID excludedPromotionId) {
        return springRepository.existsCarouselOrder(carouselOrder, excludedPromotionId);
    }

    @Override
    public long countVisibleInCarousel(UUID excludedPromotionId) {
        return springRepository.countVisibleInCarousel(excludedPromotionId);
    }

    @Override
    public List<Promotion> findAllInCarousel() {
        return springRepository.findAllByShowInCarouselTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void saveAll(List<Promotion> promotions) {
        // Clear all carousel orders first to avoid unique constraint conflicts during reorder,
        // then flush to materialise the NULLs before setting the new values.
        List<UUID> ids = promotions.stream().map(Promotion::getId).toList();
        springRepository.clearCarouselOrders(ids);
        springRepository.flush();
        promotions.forEach(this::save);
    }

    @Override
    public void deleteById(UUID id) {
        springRepository.deleteById(id);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private Promotion toDomain(PromotionJpaEntity entity) {
        Map<String, PromotionTranslation> translations = new HashMap<>();
        entity.getTranslations().forEach(t ->
                translations.put(t.getLocale(), new PromotionTranslation(t.getMarketingText()))
        );

        if (!translations.containsKey("fr")) {
            translations.put("fr", new PromotionTranslation(""));
        }

        return Promotion.reconstitute(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getDiscountPercent(),
                translations,
                entity.getStartAt(),
                entity.getEndAt(),
                entity.isEnabled(),
                entity.isShowInCarousel(),
                entity.getCarouselOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
