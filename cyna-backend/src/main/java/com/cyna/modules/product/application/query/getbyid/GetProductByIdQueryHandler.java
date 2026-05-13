package com.cyna.modules.product.application.query.getbyid;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Base64;

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
    public ProductReadModel handle(GetProductByIdQuery query) {
        return productRepository.findById(query.id()).map(product -> {
            String categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(Category::getName).orElse("Unknown");
            var images = productImageRepository.findByProductId(product.getId()).stream()
                    .map(img -> new ProductImageReadModel(
                            img.getId(),
                            Base64.getEncoder().encodeToString(img.getImageData())
                    ))
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
                    images,
                    product.getCreatedAt(),
                    product.getUpdatedAt()
            );
        }).orElse(null);
    }
}
