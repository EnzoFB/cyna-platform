package com.cyna.modules.product.application.query.getcategorybyid;

import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class GetCategoryByIdQueryHandler implements QueryHandler<GetCategoryByIdQuery, CategoryReadModel> {

    private final CategoryRepository categoryRepository;

    public GetCategoryByIdQueryHandler(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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
                        category.getCreatedAt(),
                        category.getUpdatedAt()
                ))
                .orElse(null);
    }
}
