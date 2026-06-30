package com.cyna.modules.product.application.query.listcategories;

import com.cyna.modules.product.application.query.getcategorybyid.CategoryReadModel;
import com.cyna.modules.product.application.translation.CategoryTranslationDto;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListCategoriesQueryHandler implements QueryHandler<ListCategoriesQuery, List<CategoryReadModel>> {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public ListCategoriesQueryHandler(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<CategoryReadModel> handle(ListCategoriesQuery query) {
        var stream = categoryRepository.findAll().stream();
        if (Boolean.TRUE.equals(query.activeOnly())) {
            stream = stream.filter(com.cyna.modules.product.domain.model.Category::isActive);
        }
        return stream.map(category -> new CategoryReadModel(
                        category.getId(),
                        category.getName(),
                        CategoryTranslationDto.fromDomainMap(category.getTranslations()),
                        category.getImage(),
                        category.isActive(),
                        productRepository.countByCategoryId(category.getId()),
                        category.getCreatedAt(),
                        category.getUpdatedAt()
                ))
                .toList();
    }
}
