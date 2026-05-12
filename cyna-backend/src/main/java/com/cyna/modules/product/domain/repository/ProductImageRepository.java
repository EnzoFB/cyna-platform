package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.ProductImage;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductImageRepository {

    List<ProductImage> findByProductId(UUID productId);

    List<ProductImage> findByProductIds(Collection<UUID> productIds);
}
