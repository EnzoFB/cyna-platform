package com.cyna.modules.subscription.domain.model;

import com.cyna.modules.subscription.domain.event.SubscriptionRenewed;
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
                nextBilling,
                null,
                null
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
                nextBilling,
                null,
                null
        );

        Result<Subscription> cancelled = subscription.cancelAtPeriodEnd();

        assertThat(cancelled.isSuccess()).isTrue();
        assertThat(cancelled.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(cancelled.getValue().getCancelledAt()).isNotNull();
    }

    @Test
    void should_renew_active_subscription_and_extend_periods() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        Instant nextBilling = end;

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "EDR Advanced", "EDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(99.99), "EUR"),
                start, end, nextBilling,
                "sub_abc", "sch_abc"
        );

        Instant newEnd = end.plus(30, ChronoUnit.DAYS);
        Result<Subscription> renewed = subscription.renew(newEnd, newEnd);

        assertThat(renewed.isSuccess()).isTrue();
        assertThat(renewed.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(renewed.getValue().getEndAt()).isEqualTo(newEnd);
        assertThat(renewed.getValue().getNextBillingAt()).isEqualTo(newEnd);
        assertThat(renewed.getValue().getStripeSubscriptionId()).isEqualTo("sub_abc");
        assertThat(renewed.getValue().getDomainEvents())
                .anyMatch(e -> e instanceof SubscriptionRenewed);
    }

    @Test
    void should_renew_past_due_subscription_and_recover_to_active() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "EDR", "EDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(50), "EUR"),
                start, end, end, "sub_x", "sch_x"
        );
        Subscription pastDue = subscription.markPastDue().getValue();
        assertThat(pastDue.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);

        Instant newEnd = end.plus(30, ChronoUnit.DAYS);
        Result<Subscription> renewed = pastDue.renew(newEnd, newEnd);

        assertThat(renewed.isSuccess()).isTrue();
        assertThat(renewed.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void should_reject_renewal_when_already_cancelled() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "EDR", "EDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(50), "EUR"),
                start, end, end, null, null
        );
        Subscription cancelled = subscription.cancelAtPeriodEnd().getValue();

        Result<Subscription> renewed = cancelled.renew(end.plus(30, ChronoUnit.DAYS),
                end.plus(30, ChronoUnit.DAYS));

        assertThat(renewed.isFailure()).isTrue();
    }

    @Test
    void should_reject_renewal_when_new_end_moves_backwards() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "EDR", "EDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(50), "EUR"),
                start, end, end, null, null
        );

        Result<Subscription> renewed = subscription.renew(start, start);

        assertThat(renewed.isFailure()).isTrue();
    }

    @Test
    void should_mark_active_subscription_as_past_due() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "EDR", "EDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(50), "EUR"),
                start, end, end, null, null
        );

        Result<Subscription> pastDue = subscription.markPastDue();

        assertThat(pastDue.isSuccess()).isTrue();
        assertThat(pastDue.getValue().getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        // periods preserved (Stripe is still trying to collect)
        assertThat(pastDue.getValue().getEndAt()).isEqualTo(end);
    }

    @Test
    void should_reject_mark_past_due_when_cancelled() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "EDR", "EDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(50), "EUR"),
                start, end, end, null, null
        );
        Subscription cancelled = subscription.cancelAtPeriodEnd().getValue();

        Result<Subscription> pastDue = cancelled.markPastDue();

        assertThat(pastDue.isFailure()).isTrue();
    }
}
