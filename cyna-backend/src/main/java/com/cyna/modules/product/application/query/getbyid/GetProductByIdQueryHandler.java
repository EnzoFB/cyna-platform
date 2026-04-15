package com.cyna.modules.product.application.query.getbyid;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class GetProductByIdQueryHandler implements QueryHandler<GetProductByIdQuery, ProductReadModel> {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public GetProductByIdQueryHandler(ProductRepository productRepository,
                                      CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public ProductReadModel handle(GetProductByIdQuery query) {
        return productRepository.findById(query.id()).map(product -> {
            String categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(Category::getName).orElse("Unknown");

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
                    product.getCreatedAt(),
                    product.getUpdatedAt()
            );
        }).orElse(null);
    }
}
