package com.cyna.modules.cart.infrastructure.persistence.mapper;

import com.cyna.shared.domain.BillingCycle;
import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.modules.cart.infrastructure.persistence.entity.CartJpaEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartJpaMapperTest {

    private final CartJpaMapper mapper = new CartJpaMapper();

    @Test
    void should_map_cart_domain_to_jpa_and_back() {
        Cart domain = Cart.createForUser(UUID.randomUUID())
                .addOrMergeLine(UUID.randomUUID(), "Cyna EDR 1", "EDR", BillingCycle.MONTHLY, 2)
                .getValue();

        CartJpaEntity jpa = mapper.toJpa(domain);
        Cart mappedBack = mapper.toDomain(jpa);

        assertThat(mappedBack.getId()).isEqualTo(domain.getId());
        assertThat(mappedBack.getUserId()).isEqualTo(domain.getUserId());
        assertThat(mappedBack.getStatus()).isEqualTo(domain.getStatus());
        assertThat(mappedBack.getLines()).hasSize(1);
        assertThat(mappedBack.getLines().getFirst().getProductId())
                .isEqualTo(domain.getLines().getFirst().getProductId());
        assertThat(mappedBack.getLines().getFirst().getQuantity())
                .isEqualTo(domain.getLines().getFirst().getQuantity());
    }
}
