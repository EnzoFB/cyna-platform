package com.cyna.modules.product.domain.model;

import com.cyna.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void should_create_product_in_draft_status() {
        Product product = Product.create(
                "SOC Standard",
                ProductCategory.SOC,
                ProductPriority.NORMALE,
                "Managed SOC service",
                "24/7 monitoring and incident response",
                Money.of(299.99, "EUR"),
                Money.of(2999.99, "EUR")
        );

        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("SOC Standard");
        assertThat(product.getCategory()).isEqualTo(ProductCategory.SOC);
        assertThat(product.getPriority()).isEqualTo(ProductPriority.NORMALE);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getDomainEvents()).isEmpty();
        assertThat(product.getCreatedAt()).isNotNull();
        assertThat(product.getUpdatedAt()).isNotNull();
    }

    @Test
    void should_reconstitute_product_without_events() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(3600);
        Instant updatedAt = Instant.now();

        Product product = Product.reconstitute(
                id,
                "EDR Pro",
                ProductCategory.EDR,
                ProductPriority.MOYENNE,
                "Endpoint detection and response service",
                "Behavioral analysis and host isolation",
                Money.of(199.99, "EUR"),
                Money.of(1999.99, "EUR"),
                ProductStatus.PUBLISHED,
                createdAt,
                updatedAt
        );

        assertThat(product.getId()).isEqualTo(id);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(product.getDomainEvents()).isEmpty();
    }

    @Test
    void should_reject_mixed_currencies() {
        assertThatThrownBy(() -> Product.create(
                "XDR Ultimate",
                ProductCategory.XDR,
                ProductPriority.HAUTE,
                "Extended detection and response service",
                "Cross-domain telemetry and response automation",
                Money.of(399.99, "EUR"),
                Money.of(3999.99, "USD")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
