package com.cyna.modules.product.application.query.list;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.repository.CategoryRepository;
import com.cyna.modules.product.domain.repository.ProductImageRepository;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.QueryHandler;
import com.cyna.shared.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ListProductsQueryHandler implements QueryHandler<ListProductsQuery, Page<ProductReadModel>> {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    public ListProductsQueryHandler(ProductRepository productRepository,
                                    CategoryRepository categoryRepository,
                                    ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
    }

    @Override
    public Page<ProductReadModel> handle(ListProductsQuery query) {
        int safePage = Math.max(0, query.page());
        int safeSize = Math.min(Math.max(1, query.size()), 100);

        var page = productRepository.findAll(
                safePage,
                safeSize,
                query.published(),
                query.categoryId(),
                query.search(),
                query.sort()
        );

        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<UUID, java.util.List<String>> productImages = productImageRepository.findByProductIds(
                        page.items().stream().map(product -> product.getId()).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        image -> image.getProductId(),
                        Collectors.mapping(image -> image.getImageUrl(), Collectors.toList())
                ));

        var items = page.items().stream().map(product -> new ProductReadModel(
                product.getId(),
                product.getName(),
                product.getCategoryId(),
                categoryNames.getOrDefault(product.getCategoryId(), "Unknown"),
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
                productImages.getOrDefault(product.getId(), java.util.List.of()),
                product.getCreatedAt(),
                product.getUpdatedAt()
        )).toList();

        return new Page<>(items, page.pageNumber(), page.pageSize(), page.totalElements(), page.totalPages());
    }
}
