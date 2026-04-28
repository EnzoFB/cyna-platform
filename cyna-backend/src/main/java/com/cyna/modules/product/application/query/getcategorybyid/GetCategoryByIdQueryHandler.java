package com.cyna.modules.product.application.query.getcategorybyid;

import com.cyna.modules.product.application.cache.ProductCacheNames;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class GetCategoryByIdQueryHandler implements QueryHandler<GetCategoryByIdQuery, CategoryReadModel> {

    private final CategoryRepository categoryRepository;

    public GetCategoryByIdQueryHandler(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Cacheable(
            cacheNames = ProductCacheNames.CATEGORY_BY_ID,
            key = "#query.id()",
            unless = "#result == null"
    )
    public CategoryReadModel handle(GetCategoryByIdQuery query) {
        return categoryRepository.findById(query.id())
                .map(category -> new CategoryReadModel(
                        category.getId(),
                        category.getName(),
                        category.getDescription(),
                        category.getImage(),
                        category.isActive(),
                        category.getCreatedAt(),
                        category.getUpdatedAt()
                ))
                .orElse(null);
    }
}
