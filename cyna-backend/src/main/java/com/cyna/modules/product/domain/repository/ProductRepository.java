package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.application.query.list.ProductSort;
import com.cyna.shared.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    void save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAllByIds(List<UUID> ids);

    Page<Product> findAll(int page, int size, Boolean published, UUID categoryId, String search, ProductSort sort);

    boolean existsById(UUID id);

    void deleteById(UUID id);
}
