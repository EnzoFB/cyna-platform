package com.cyna.modules.product.application.query.list;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.QueryHandler;
import com.cyna.shared.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ListProductsQueryHandler implements QueryHandler<ListProductsQuery, Page<ProductReadModel>> {

    private final ProductRepository productRepository;

    public ListProductsQueryHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Page<ProductReadModel> handle(ListProductsQuery query) {
        int safePage = Math.max(0, query.page());
        int safeSize = Math.min(Math.max(1, query.size()), 100);

        var page = productRepository.findAll(
                safePage,
                safeSize,
                query.status(),
                query.category(),
                query.search(),
                query.sort()
        );

        var items = page.items().stream().map(product -> new ProductReadModel(
                product.getId(),
                product.getName(),
                product.getCategory().name(),
                product.getPriority().name(),
                product.getServiceDescription(),
                product.getTechnicalDescription(),
                product.getMonthlyPrice().amount(),
                product.getAnnualPrice().amount(),
                product.getMonthlyPrice().currency(),
                product.getStatus().name(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        )).toList();

        return new Page<>(items, page.pageNumber(), page.pageSize(), page.totalElements(), page.totalPages());
    }
}
