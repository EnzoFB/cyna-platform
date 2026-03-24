package com.cyna.modules.product.application.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductQueryApi {

    Optional<ProductInfo> getById(UUID productId);

    List<ProductInfo> getByIds(List<UUID> productIds);

    boolean exists(UUID productId);
}
