package com.cyna.modules.product.application.api;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
class ProductQueryApiImpl implements ProductQueryApi {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    ProductQueryApiImpl(ProductRepository productRepository,
                        CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Optional<ProductInfo> getById(UUID productId) {
        return productRepository.findById(productId).map(product -> {
            String categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(Category::getName).orElse("Unknown");
            return new ProductInfo(
                    product.getId(),
                    product.getName(),
                    product.getServiceDescription(),
                    product.getTechnicalDescription(),
                    product.getMonthlyPrice(),
                    product.getAnnualPrice(),
                    product.getCurrency(),
                    product.isPublished(),
                    product.isAvailable(),
                    product.getCategoryId(),
                    categoryName,
                    product.getPriorityLevel()
            );
        });
    }

    @Override
    public List<ProductInfo> getByIds(List<UUID> productIds) {
        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return productRepository.findAllByIds(productIds).stream().map(product -> new ProductInfo(
                product.getId(),
                product.getName(),
                product.getServiceDescription(),
                product.getTechnicalDescription(),
                product.getMonthlyPrice(),
                product.getAnnualPrice(),
                product.getCurrency(),
                product.isPublished(),
                product.isAvailable(),
                product.getCategoryId(),
                categoryNames.getOrDefault(product.getCategoryId(), "Unknown"),
                product.getPriorityLevel()
        )).toList();
    }

    @Override
    public boolean exists(UUID productId) {
        return productRepository.existsById(productId);
    }
}
