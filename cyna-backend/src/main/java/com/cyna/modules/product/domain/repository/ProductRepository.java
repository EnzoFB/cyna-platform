package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.shared.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    void save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAllByIds(List<UUID> ids);

    Page<Product> findAll(int page, int size, String status, String category, String search, String sort);

    boolean existsById(UUID id);

    void deleteById(UUID id);
}
