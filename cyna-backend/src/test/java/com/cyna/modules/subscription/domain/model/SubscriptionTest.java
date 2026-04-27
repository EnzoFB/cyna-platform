package com.cyna.modules.subscription.domain.model;

import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTest {

    @Test
    void should_create_active_subscription() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        Instant nextBilling = start.plus(30, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "EDR Advanced",
                "EDR",
                BillingCycle.MONTHLY,
                2,
                Money.of(BigDecimal.valueOf(199.99), "EUR"),
                start,
                end,
                nextBilling
        );

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getQuantity()).isEqualTo(2);
        assertThat(subscription.getUnitPrice().currency()).isEqualTo("EUR");
    }

    @Test
    void should_cancel_at_period_end() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        Instant nextBilling = start.plus(30, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "XDR Pro",
                "XDR",
                BillingCycle.MONTHLY,
                1,
                Money.of(BigDecimal.valueOf(299.99), "EUR"),
                start,
                end,
                nextBilling
        );

        Result<Subscription> cancelled = subscription.cancelAtPeriodEnd();

        assertThat(cancelled.isSuccess()).isTrue();
        assertThat(cancelled.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(cancelled.getValue().getCancelledAt()).isNotNull();
    }
}
