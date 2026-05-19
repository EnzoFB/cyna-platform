package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.Promotion;
import com.cyna.modules.product.domain.repository.PromotionRepository;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.PromotionJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPromotionRepositoryAdapter implements PromotionRepository {

    private final SpringDataPromotionRepository springRepository;
    private final SpringDataProductRepository productRepository;

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
        entity.setMarketingTextFr(promotion.getMarketingTextFr());
        entity.setMarketingTextEn(promotion.getMarketingTextEn());
        entity.setStartAt(promotion.getStartAt());
        entity.setEndAt(promotion.getEndAt());
        entity.setEnabled(promotion.isEnabled());
        entity.setCreatedAt(promotion.getCreatedAt());
        entity.setUpdatedAt(promotion.getUpdatedAt());
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
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return springRepository.findAllByProduct_IdIn(productIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Promotion> findActiveByProductIds(Collection<UUID> productIds, Instant atInstant) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
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
    public void deleteById(UUID id) {
        springRepository.deleteById(id);
    }

    private Promotion toDomain(PromotionJpaEntity entity) {
        return Promotion.reconstitute(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getDiscountPercent(),
                entity.getMarketingTextFr(),
                entity.getMarketingTextEn(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
