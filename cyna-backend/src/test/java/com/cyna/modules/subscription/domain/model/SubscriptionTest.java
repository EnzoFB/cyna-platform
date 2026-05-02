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

    @Test
    void should_disable_auto_renew_when_active() {
        Instant start = Instant.now();
        Instant end = start.plus(365, ChronoUnit.DAYS);
        Instant nextBilling = start.plus(365, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SOC Premium",
                "SOC",
                BillingCycle.ANNUAL,
                1,
                Money.of(BigDecimal.valueOf(999.99), "EUR"),
                start,
                end,
                nextBilling
        );

        Result<Subscription> updated = subscription.updateAutoRenew(false);

        assertThat(updated.isSuccess()).isTrue();
        assertThat(updated.getValue().isAutoRenew()).isFalse();
    }

    @Test
    void should_reject_auto_renew_update_when_not_active() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        Instant nextBilling = start.plus(30, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SOC Standard",
                "SOC",
                BillingCycle.MONTHLY,
                1,
                Money.of(BigDecimal.valueOf(99.99), "EUR"),
                start,
                end,
                nextBilling
        );
        Subscription cancelled = subscription.cancelAtPeriodEnd().getValue();

        Result<Subscription> updated = cancelled.updateAutoRenew(false);

        assertThat(updated.isFailure()).isTrue();
        assertThat(updated.getError()).contains("active");
    }

    @Test
    void should_mark_auto_renew_notice_as_sent() {
        Instant start = Instant.now();
        Instant end = start.plus(365, ChronoUnit.DAYS);
        Instant nextBilling = start.plus(365, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "XDR Pro",
                "XDR",
                BillingCycle.ANNUAL,
                1,
                Money.of(BigDecimal.valueOf(1299.99), "EUR"),
                start,
                end,
                nextBilling
        );

        Result<Subscription> result = subscription.markAutoRenewNoticeSent(Instant.now());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getAutoRenewNoticeSentAt()).isNotNull();
    }
}
