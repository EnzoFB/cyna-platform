package com.cyna.modules.order.domain.model;

import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void should_create_order_with_totals() {
        OrderLine line = OrderLine.create(
                UUID.randomUUID(),
                "EDR Advanced",
                "EDR",
                BillingCycle.MONTHLY,
                2,
                Money.of(BigDecimal.valueOf(100), "EUR"),
                0
        );

        Order order = Order.create(UUID.randomUUID(), List.of(line), null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        // HT subtotal only — Stripe owns VAT/TTC, see Order javadoc.
        assertThat(order.getSubtotalHt().amount()).isEqualByComparingTo("200");
    }

    @Test
    void should_cancel_order() {
        OrderLine line = OrderLine.create(
                UUID.randomUUID(),
                "SOC",
                "SOC",
                BillingCycle.ANNUAL,
                1,
                Money.of(BigDecimal.valueOf(500), "EUR"),
                0
        );
        Order order = Order.create(UUID.randomUUID(), List.of(line), null);

        Result<Order> cancelled = order.cancel("No longer needed");

        assertThat(cancelled.isSuccess()).isTrue();
        assertThat(cancelled.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
