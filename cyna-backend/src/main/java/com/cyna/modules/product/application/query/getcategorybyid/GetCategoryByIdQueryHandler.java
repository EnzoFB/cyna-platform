package com.cyna.modules.product.application.query.getcategorybyid;

import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class GetCategoryByIdQueryHandler implements QueryHandler<GetCategoryByIdQuery, CategoryReadModel> {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public GetCategoryByIdQueryHandler(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public CategoryReadModel handle(GetCategoryByIdQuery query) {
        return categoryRepository.findById(query.id())
                .map(category -> new CategoryReadModel(
                        category.getId(),
                        category.getName(),
                        category.getFullName(),
                        category.getDescription(),
                        category.getImage(),
                        category.isActive(),
                        productRepository.countByCategoryId(category.getId()),
                        category.getCreatedAt(),
                        category.getUpdatedAt()
                ))
                .orElse(null);
    }
}
