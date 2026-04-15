package com.cyna.modules.product.infrastructure.persistence.mapper;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductJpaMapper {

    public ProductJpaEntity toJpa(Product product) {
        var entity = new ProductJpaEntity();
        entity.setId(product.getId());
        entity.setName(product.getName());

        var categoryRef = new CategoryJpaEntity();
        categoryRef.setId(product.getCategoryId());
        entity.setCategory(categoryRef);

        entity.setPriorityLevel(product.getPriorityLevel());
        entity.setServiceDescription(product.getServiceDescription());
        entity.setTechnicalDescription(product.getTechnicalDescription());
        entity.setMonthlyPrice(product.getMonthlyPrice());
        entity.setAnnualPrice(product.getAnnualPrice());
        entity.setCurrency(product.getCurrency());
        entity.setPublished(product.isPublished());
        entity.setAvailable(product.isAvailable());
        entity.setFreeTrialDays(product.getFreeTrialDays());
        entity.setHighlightPoints(product.getHighlightPoints());
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
            entity.getMonthlyPrice(),
            entity.getAnnualPrice(),
            entity.getCurrency(),
            entity.isPublished(),
            entity.isAvailable(),
            entity.getFreeTrialDays(),
            entity.getHighlightPoints(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
