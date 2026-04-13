package com.cyna.modules.product.application.query.getbyid;

import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class GetProductByIdQueryHandler implements QueryHandler<GetProductByIdQuery, ProductReadModel> {

    private final ProductRepository productRepository;

    public GetProductByIdQueryHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductReadModel handle(GetProductByIdQuery query) {
        return productRepository.findById(query.id()).map(product -> new ProductReadModel(
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
        )).orElse(null);
    }
}
