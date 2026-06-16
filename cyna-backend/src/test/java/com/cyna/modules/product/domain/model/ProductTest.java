package com.cyna.modules.product.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    void should_create_product_unpublished_and_available() {
        Product product = Product.create(
                Map.of("fr", new ProductTranslation(
                        "SOC Standard",
                        "Managed SOC service",
                        "24/7 monitoring and incident response",
                        List.of("24/7 monitoring", "Incident response")
                )),
                CATEGORY_ID,
                1,
                BigDecimal.valueOf(299.99),
                BigDecimal.valueOf(2999.99),
                "EUR",
                14
        );

        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("SOC Standard");
        assertThat(product.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(product.getPriorityLevel()).isEqualTo(1);
        assertThat(product.isPublished()).isFalse();
        assertThat(product.isAvailable()).isTrue();
        assertThat(product.getFreeTrialDays()).isEqualTo(14);
        assertThat(product.getTranslations().get("fr").highlightPoints())
                .containsExactly("24/7 monitoring", "Incident response");
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
                Map.of("fr", new ProductTranslation(
                        "EDR Pro",
                        "Endpoint detection and response service",
                        "Behavioral analysis and host isolation",
                        List.of("Advanced threat detection")
                )),
                CATEGORY_ID,
                2,
                BigDecimal.valueOf(199.99),
                BigDecimal.valueOf(1999.99),
                "EUR",
                true,
                true,
                30,
                createdAt,
                updatedAt
        );

        assertThat(product.getId()).isEqualTo(id);
        assertThat(product.isPublished()).isTrue();
        assertThat(product.getFreeTrialDays()).isEqualTo(30);
        assertThat(product.getTranslations().get("fr").highlightPoints())
                .containsExactly("Advanced threat detection");
        assertThat(product.getDomainEvents()).isEmpty();
    }

    @Test
    void should_reject_negative_monthly_price() {
        assertThatThrownBy(() -> Product.create(
                Map.of("fr", new ProductTranslation(
                        "XDR Ultimate",
                        "Extended detection and response service",
                        "Cross-domain telemetry and response automation",
                        List.of()
                )),
                CATEGORY_ID,
                3,
                BigDecimal.valueOf(-1),
                BigDecimal.valueOf(3999.99),
                "EUR",
                0
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_negative_annual_price() {
        assertThatThrownBy(() -> Product.create(
                Map.of("fr", new ProductTranslation(
                        "XDR Ultimate",
                        "Extended detection and response service",
                        "Cross-domain telemetry and response automation",
                        List.of()
                )),
                CATEGORY_ID,
                3,
                BigDecimal.valueOf(399.99),
                BigDecimal.valueOf(-1),
                "EUR",
                0
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_have_immutable_highlight_points() {
        Product product = Product.create(
                Map.of("fr", new ProductTranslation(
                        "SOC Standard",
                        "Service desc",
                        "Tech desc",
                        List.of("Point 1", "Point 2")
                )),
                CATEGORY_ID,
                1,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1000),
                "EUR",
                0
        );

        assertThatThrownBy(() -> product.getTranslations().get("fr").highlightPoints().add("Point 3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
