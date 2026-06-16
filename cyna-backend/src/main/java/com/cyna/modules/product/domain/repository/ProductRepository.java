package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.shared.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    void save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findAllByIds(List<UUID> ids);

    Page<Product> findAll(int page,
                          int size,
                          Boolean published,
                          Boolean available,
                          UUID categoryId,
                          List<UUID> categoryIds,
                          String search,
                          BigDecimal monthlyPriceMin,
                          BigDecimal monthlyPriceMax,
                          BigDecimal annualPriceMin,
                          BigDecimal annualPriceMax,
                          Integer minFreeTrialDays,
                          ProductSort sort,
                          Boolean activeCategoryOnly);

    boolean existsById(UUID id);

    void deleteById(UUID id);

    void deleteAllByIds(List<UUID> ids);

    long countByCategoryId(UUID categoryId);

    boolean existsByCategoryIdIn(List<UUID> categoryIds);
}
