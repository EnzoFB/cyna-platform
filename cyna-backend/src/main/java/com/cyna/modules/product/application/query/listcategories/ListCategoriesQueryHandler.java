package com.cyna.modules.product.application.query.listcategories;

import com.cyna.modules.product.application.query.getcategorybyid.CategoryReadModel;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListCategoriesQueryHandler implements QueryHandler<ListCategoriesQuery, List<CategoryReadModel>> {

    private final CategoryRepository categoryRepository;

    public ListCategoriesQueryHandler(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryReadModel> handle(ListCategoriesQuery query) {
        return categoryRepository.findAll().stream()
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
                .toList();
    }
}
