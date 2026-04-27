package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.ProductImage;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class JpaProductImageRepositoryAdapter implements ProductImageRepository {

    private final SpringDataProductImageRepository springRepo;

    public JpaProductImageRepositoryAdapter(SpringDataProductImageRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public List<ProductImage> findByProductId(UUID productId) {
        return springRepo.findAllByProduct_IdOrderByDisplayOrderAsc(productId).stream()
                .map(entity -> ProductImage.reconstitute(
                        entity.getId(),
                        entity.getProduct().getId(),
                        entity.getImageUrl(),
                        entity.getDisplayOrder(),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()
                ))
                .toList();
    }

    @Override
    public List<ProductImage> findByProductIds(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        return springRepo.findAllByProduct_IdInOrderByProduct_IdAscDisplayOrderAsc(productIds).stream()
                .map(entity -> ProductImage.reconstitute(
                        entity.getId(),
                        entity.getProduct().getId(),
                        entity.getImageUrl(),
                        entity.getDisplayOrder(),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()
                ))
                .toList();
    }
}

