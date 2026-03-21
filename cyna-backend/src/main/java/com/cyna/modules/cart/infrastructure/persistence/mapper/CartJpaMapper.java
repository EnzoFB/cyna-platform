package com.cyna.modules.cart.infrastructure.persistence.mapper;

import com.cyna.modules.cart.domain.model.BillingCycle;
import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.modules.cart.domain.model.CartLine;
import com.cyna.modules.cart.domain.model.CartStatus;
import com.cyna.modules.cart.infrastructure.persistence.entity.CartJpaEntity;
import com.cyna.modules.cart.infrastructure.persistence.entity.CartLineJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CartJpaMapper {

    public CartJpaEntity toJpa(Cart cart) {
        CartJpaEntity entity = new CartJpaEntity();
        entity.setId(cart.getId());
        entity.setUserId(cart.getUserId());
        entity.setGuestToken(cart.getGuestToken());
        entity.setStatus(cart.getStatus().name());
        entity.setCreatedAt(cart.getCreatedAt());
        entity.setUpdatedAt(cart.getUpdatedAt());
        entity.setLines(cart.getLines().stream().map(this::toJpa).toList());
        return entity;
    }

    public Cart toDomain(CartJpaEntity entity) {
        List<CartLine> lines = entity.getLines().stream().map(this::toDomain).toList();

        return Cart.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getGuestToken(),
                CartStatus.valueOf(entity.getStatus()),
                lines,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private CartLineJpaEntity toJpa(CartLine line) {
        CartLineJpaEntity entity = new CartLineJpaEntity();
        entity.setId(line.getId());
        entity.setProductId(line.getProductId());
        entity.setProductName(line.getProductName());
        entity.setProductCategory(line.getProductCategory());
        entity.setBillingCycle(line.getBillingCycle().name());
        entity.setQuantity(line.getQuantity());
        return entity;
    }

    private CartLine toDomain(CartLineJpaEntity entity) {
        return CartLine.reconstitute(
                entity.getId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getProductCategory(),
                BillingCycle.valueOf(entity.getBillingCycle()),
                entity.getQuantity()
        );
    }
}
