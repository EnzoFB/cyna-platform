package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.ProductImage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository {

    List<ProductImage> findByProductId(UUID productId);

    List<ProductImage> findByProductIds(Collection<UUID> productIds);

    Optional<ProductImage> findById(UUID id);

    ProductImage save(ProductImage image);

    void deleteById(UUID id);

    int countByProductId(UUID productId);

    int maxDisplayOrderByProductId(UUID productId);

    void decrementDisplayOrderAfter(UUID productId, int deletedOrder);

    void updateDisplayOrder(UUID id, int newOrder);
}
