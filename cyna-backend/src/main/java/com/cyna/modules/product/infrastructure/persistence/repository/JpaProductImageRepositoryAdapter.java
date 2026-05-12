package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.ProductImage;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductImageJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProductImageRepositoryAdapter implements ProductImageRepository {

    private final SpringDataProductImageRepository springRepo;
    private final SpringDataProductRepository productRepo;

    public JpaProductImageRepositoryAdapter(SpringDataProductImageRepository springRepo,
                                            SpringDataProductRepository productRepo) {
        this.springRepo = springRepo;
        this.productRepo = productRepo;
    }

    @Override
    public List<ProductImage> findByProductId(UUID productId) {
        return springRepo.findAllByProduct_IdOrderByDisplayOrderAsc(productId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ProductImage> findByProductIds(Collection<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return springRepo.findAllByProduct_IdInOrderByProduct_IdAscDisplayOrderAsc(productIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ProductImage> findById(UUID id) {
        return springRepo.findById(id).map(this::toDomain);
    }

    @Override
    public ProductImage save(ProductImage image) {
        ProductJpaEntity product = productRepo.getReferenceById(image.getProductId());
        ProductImageJpaEntity entity = springRepo.findById(image.getId())
                .orElseGet(ProductImageJpaEntity::new);
        entity.setId(image.getId());
        entity.setProduct(product);
        entity.setImageData(image.getImageData());
        entity.setMimeType(image.getMimeType());
        entity.setDisplayOrder(image.getDisplayOrder());
        entity.setCreatedAt(image.getCreatedAt());
        entity.setUpdatedAt(Instant.now());
        return toDomain(springRepo.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        springRepo.deleteById(id);
    }

    @Override
    public int countByProductId(UUID productId) {
        return springRepo.countByProduct_Id(productId);
    }

    @Override
    public int maxDisplayOrderByProductId(UUID productId) {
        return springRepo.findMaxDisplayOrderByProductId(productId);
    }

    private ProductImage toDomain(ProductImageJpaEntity entity) {
        return ProductImage.reconstitute(
                entity.getId(),
                entity.getProduct().getId(),
                entity.getImageData(),
                entity.getMimeType(),
                entity.getDisplayOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
