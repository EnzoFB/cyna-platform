package com.cyna.modules.product.application.api;

import com.cyna.modules.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ProductQueryApiImpl implements ProductQueryApi {

    private final ProductRepository productRepository;

    ProductQueryApiImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<ProductInfo> getById(UUID productId) {
        return productRepository.findById(productId).map(product -> new ProductInfo(
                product.getId(),
                product.getName(),
                product.getServiceDescription(),
                product.getTechnicalDescription(),
                product.getMonthlyPrice().amount(),
                product.getAnnualPrice().amount(),
                product.getMonthlyPrice().currency(),
                product.getStatus().name(),
                product.getCategory().name(),
                product.getPriority().name()
        ));
    }

    @Override
    public List<ProductInfo> getByIds(List<UUID> productIds) {
        return productRepository.findAllByIds(productIds).stream().map(product -> new ProductInfo(
                product.getId(),
                product.getName(),
                product.getServiceDescription(),
                product.getTechnicalDescription(),
                product.getMonthlyPrice().amount(),
                product.getAnnualPrice().amount(),
                product.getMonthlyPrice().currency(),
                product.getStatus().name(),
                product.getCategory().name(),
                product.getPriority().name()
        )).toList();
    }

    @Override
    public boolean exists(UUID productId) {
        return productRepository.existsById(productId);
    }
}
