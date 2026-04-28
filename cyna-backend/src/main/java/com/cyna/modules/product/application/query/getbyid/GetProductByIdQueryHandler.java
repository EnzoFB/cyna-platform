package com.cyna.modules.product.application.query.getbyid;

import com.cyna.modules.product.application.cache.ProductCacheNames;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class GetProductByIdQueryHandler implements QueryHandler<GetProductByIdQuery, ProductReadModel> {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    public GetProductByIdQueryHandler(ProductRepository productRepository,
                                      CategoryRepository categoryRepository,
                                      ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
    }

    @Override
    @Cacheable(
            cacheNames = ProductCacheNames.PRODUCT_BY_ID,
            key = "#query.id()",
            unless = "#result == null"
    )
    public ProductReadModel handle(GetProductByIdQuery query) {
        return productRepository.findById(query.id()).map(product -> {
            String categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(Category::getName).orElse("Unknown");
            var imageUrls = productImageRepository.findByProductId(product.getId()).stream()
                    .map(productImage -> productImage.getImageUrl())
                    .toList();

            return new ProductReadModel(
                    product.getId(),
                    product.getName(),
                    product.getCategoryId(),
                    categoryName,
                    product.getPriorityLevel(),
                    product.getServiceDescription(),
                    product.getTechnicalDescription(),
                    product.getMonthlyPrice(),
                    product.getAnnualPrice(),
                    product.getCurrency(),
                    product.isPublished(),
                    product.isAvailable(),
                    product.getFreeTrialDays(),
                    product.getHighlightPoints(),
                    imageUrls,
                    product.getCreatedAt(),
                    product.getUpdatedAt()
            );
        }).orElse(null);
    }
}
