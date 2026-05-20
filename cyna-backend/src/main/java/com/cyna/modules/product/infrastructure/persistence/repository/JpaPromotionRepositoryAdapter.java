package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.PromotionJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.PromotionTranslationJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

        // Build translation rows — FR always present, EN always present
        // (domain validates both are non-blank)
        Set<PromotionTranslationJpaEntity> translations = new HashSet<>();
        translations.add(PromotionTranslationJpaEntity.of(entity, "fr", promotion.getMarketingTextFr()));
        if (promotion.getMarketingTextEn() != null && !promotion.getMarketingTextEn().isBlank()) {
            translations.add(PromotionTranslationJpaEntity.of(entity, "en", promotion.getMarketingTextEn()));
        }
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
    public void deleteById(UUID id) {
        springRepository.deleteById(id);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private Promotion toDomain(PromotionJpaEntity entity) {
        PromotionTranslationJpaEntity fr = findLocale(entity, "fr");
        PromotionTranslationJpaEntity en = findLocale(entity, "en");

        String marketingTextFr = fr != null ? fr.getMarketingText() : "";
        // Fall back to FR text so domain Guard.againstNullOrBlank never fails
        String marketingTextEn = (en != null && !en.getMarketingText().isBlank())
                ? en.getMarketingText()
                : marketingTextFr;

        return Promotion.reconstitute(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getDiscountPercent(),
                marketingTextFr,
                marketingTextEn,
                entity.getStartAt(),
                entity.getEndAt(),
                entity.isEnabled(),
                entity.isShowInCarousel(),
                entity.getCarouselOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static PromotionTranslationJpaEntity findLocale(PromotionJpaEntity entity, String locale) {
        return entity.getTranslations().stream()
                .filter(t -> locale.equals(t.getLocale()))
                .findFirst()
                .orElse(null);
    }
}
