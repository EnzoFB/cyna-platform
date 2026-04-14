package com.cyna.modules.product.infrastructure.persistence.mapper;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.shared.domain.Money;
import org.springframework.stereotype.Component;

@Component
public class ProductJpaMapper {

    public ProductJpaEntity toJpa(Product product, CategoryJpaEntity category) {
        var entity = new ProductJpaEntity();
        entity.setId(product.getId());
        entity.setName(product.getName());
        entity.setCategory(category);
        entity.setPriorityLevel(product.getPriorityLevel());
        entity.setServiceDescription(product.getServiceDescription());
        entity.setTechnicalDescription(product.getTechnicalDescription());
        entity.setMonthlyPrice(product.getMonthlyPrice().amount());
        entity.setAnnualPrice(product.getAnnualPrice().amount());
        entity.setCurrency(product.getMonthlyPrice().currency());
        entity.setPublished(product.isPublished());
        entity.setAvailable(product.isAvailable());
        entity.setCreatedAt(product.getCreatedAt());
        entity.setUpdatedAt(product.getUpdatedAt());
        return entity;
    }

    public Product toDomain(ProductJpaEntity entity) {
        return Product.reconstitute(
            entity.getId(),
            entity.getName(),
            entity.getCategory().getId(),
            entity.getPriorityLevel(),
            entity.getServiceDescription(),
            entity.getTechnicalDescription(),
            Money.of(entity.getMonthlyPrice(), entity.getCurrency()),
            Money.of(entity.getAnnualPrice(), entity.getCurrency()),
            entity.isPublished(),
            entity.isAvailable(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
