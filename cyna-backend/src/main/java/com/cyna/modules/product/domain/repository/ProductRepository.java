package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    void save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAllByIds(List<UUID> ids);

    boolean existsById(UUID id);
}
