package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    void save(Category category);

    Optional<Category> findById(UUID id);

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    List<Category> findAll();

    void deleteById(UUID id);
}
