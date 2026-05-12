package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.application.query.list.ProductSort;
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
                          ProductSort sort);

    boolean existsById(UUID id);

    void deleteById(UUID id);

    long countByCategoryId(UUID categoryId);
}
