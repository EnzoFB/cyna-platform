package com.cyna.modules.product.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    void should_create_product_unpublished_and_available() {
        Product product = Product.create(
                "SOC Standard",
                CATEGORY_ID,
                1,
                "Managed SOC service",
                "24/7 monitoring and incident response",
                BigDecimal.valueOf(299.99),
                BigDecimal.valueOf(2999.99),
                "EUR",
                14,
                List.of("24/7 monitoring", "Incident response")
        );

        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("SOC Standard");
        assertThat(product.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(product.getPriorityLevel()).isEqualTo(1);
        assertThat(product.isPublished()).isFalse();
        assertThat(product.isAvailable()).isTrue();
        assertThat(product.getFreeTrialDays()).isEqualTo(14);
        assertThat(product.getHighlightPoints()).containsExactly("24/7 monitoring", "Incident response");
        assertThat(product.getCurrency()).isEqualTo("EUR");
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
                CATEGORY_ID,
                2,
                "Endpoint detection and response service",
                "Behavioral analysis and host isolation",
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                true,
                true,
                30,
                List.of("Advanced threat detection"),
                createdAt,
                updatedAt
        );

        assertThat(product.getId()).isEqualTo(id);
        assertThat(product.isPublished()).isTrue();
        assertThat(product.getFreeTrialDays()).isEqualTo(30);
        assertThat(product.getHighlightPoints()).containsExactly("Advanced threat detection");
        assertThat(product.getDomainEvents()).isEmpty();
    }

    @Test
    void should_reject_negative_monthly_price() {
        assertThatThrownBy(() -> Product.create(
                "XDR Ultimate",
                CATEGORY_ID,
                3,
                "Extended detection and response service",
                "Cross-domain telemetry and response automation",
                BigDecimal.valueOf(-1),
                BigDecimal.valueOf(3999.99),
                "EUR",
                0,
                List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_negative_annual_price() {
        assertThatThrownBy(() -> Product.create(
                "XDR Ultimate",
                CATEGORY_ID,
                3,
                "Extended detection and response service",
                "Cross-domain telemetry and response automation",
                BigDecimal.valueOf(399.99),
                BigDecimal.valueOf(-1),
                "EUR",
                0,
                List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_have_immutable_highlight_points() {
        List<String> points = new java.util.ArrayList<>(List.of("Point 1", "Point 2"));
        Product product = Product.create(
                "SOC Standard",
                CATEGORY_ID,
                1,
                "Service desc",
                "Tech desc",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1000),
                "EUR",
                0,
                points
        );

        assertThatThrownBy(() -> product.getHighlightPoints().add("Point 3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
