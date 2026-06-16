package com.cyna.modules.subscription.domain.model;

import com.cyna.shared.domain.BillingCycle;

import com.cyna.modules.subscription.domain.event.SubscriptionCancelled;
import com.cyna.modules.subscription.domain.event.SubscriptionPaymentActionRequired;
import com.cyna.modules.subscription.domain.event.SubscriptionRenewed;
import com.cyna.shared.domain.DomainEvent;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
                null,
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
        assertThat(subscription.isAutoRenew()).isTrue();
    }

    @Test
    void cancel_at_period_end_keeps_status_active_and_disables_auto_renew() {
        Subscription subscription = monthlySubscription();

        Result<Subscription> cancelled = subscription.cancelAtPeriodEnd();

        assertThat(cancelled.isSuccess()).isTrue();
        // Status stays ACTIVE — customer paid for the current period and keeps access
        // until Stripe emits customer.subscription.deleted at period end.
        assertThat(cancelled.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(cancelled.getValue().isAutoRenew()).isFalse();
        assertThat(cancelled.getValue().getCancelledAt()).isNotNull();
        assertThat(cancelled.getValue().getDomainEvents())
                .noneMatch(e -> e instanceof SubscriptionCancelled);
    }

    @Test
    void re_enabling_auto_renew_clears_cancelled_marker() {
        Subscription scheduled = monthlySubscription().cancelAtPeriodEnd().getValue();
        assertThat(scheduled.getCancelledAt()).isNotNull();

        Result<Subscription> resumed = scheduled.updateAutoRenew(true);

        assertThat(resumed.isSuccess()).isTrue();
        assertThat(resumed.getValue().isAutoRenew()).isTrue();
        assertThat(resumed.getValue().getCancelledAt()).isNull();
    }

    @Test
    void mark_fully_cancelled_transitions_to_cancelled_status_and_raises_event() {
        Subscription scheduled = monthlySubscription().cancelAtPeriodEnd().getValue();

        Result<Subscription> terminal = scheduled.markFullyCancelled();

        assertThat(terminal.isSuccess()).isTrue();
        assertThat(terminal.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(terminal.getValue().getDomainEvents())
                .anyMatch(e -> e instanceof SubscriptionCancelled);
    }

    @Test
    void mark_fully_cancelled_is_idempotent() {
        Subscription cancelled = monthlySubscription().markFullyCancelled().getValue();

        Result<Subscription> second = cancelled.markFullyCancelled();

        assertThat(second.isSuccess()).isTrue();
        assertThat(second.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void should_renew_active_subscription_and_extend_periods() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        Instant nextBilling = end;

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
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
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
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
    void should_reject_renewal_when_fully_cancelled() {
        Subscription cancelled = monthlySubscription().markFullyCancelled().getValue();

        Result<Subscription> renewed = cancelled.renew(
                cancelled.getEndAt().plus(30, ChronoUnit.DAYS),
                cancelled.getEndAt().plus(30, ChronoUnit.DAYS)
        );

        assertThat(renewed.isFailure()).isTrue();
    }

    @Test
    void should_reject_renewal_when_new_end_moves_backwards() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);

        Subscription subscription = Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
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
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
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
    void should_reject_mark_past_due_when_fully_cancelled() {
        Subscription cancelled = monthlySubscription().markFullyCancelled().getValue();

        Result<Subscription> pastDue = cancelled.markPastDue();

        assertThat(pastDue.isFailure()).isTrue();
    }

    // ── markPaymentActionRequired (PSD2 SCA on renewal) ───────────────────────

    @Test
    void should_mark_active_subscription_as_payment_action_required_and_raise_event_with_url() {
        Subscription subscription = monthlySubscription();
        String hostedUrl = "https://invoice.stripe.com/i/test_inv_abc";

        Result<Subscription> result = subscription.markPaymentActionRequired(hostedUrl);

        assertThat(result.isSuccess()).isTrue();
        Subscription pastDue = result.getValue();
        assertThat(pastDue.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);

        List<DomainEvent> events = pastDue.getDomainEvents();
        assertThat(events).singleElement().isInstanceOf(SubscriptionPaymentActionRequired.class);
        SubscriptionPaymentActionRequired event = (SubscriptionPaymentActionRequired) events.get(0);
        assertThat(event.subscriptionId()).isEqualTo(pastDue.getId());
        assertThat(event.userId()).isEqualTo(pastDue.getUserId());
        assertThat(event.productId()).isEqualTo(pastDue.getProductId());
        assertThat(event.hostedInvoiceUrl()).isEqualTo(hostedUrl);
    }

    /**
     * Regression guard for the dunning-spam bug: Stripe re-emits
     * {@code invoice.payment_action_required} at every dunning retry
     * (24-72 h cadence). Without the {@code PAST_DUE → PAST_DUE} no-op the
     * customer would receive a fresh "complete 3DS" email at every retry.
     * The first transition still raises the event; subsequent calls while
     * already PAST_DUE must be silent.
     */
    @Test
    void should_be_a_silent_noop_when_called_again_while_already_past_due() {
        Subscription subscription = monthlySubscription();
        Subscription firstTransition = subscription
                .markPaymentActionRequired("https://invoice.stripe.com/first")
                .getValue();
        firstTransition.clearDomainEvents();

        Result<Subscription> second =
                firstTransition.markPaymentActionRequired("https://invoice.stripe.com/second");

        assertThat(second.isSuccess()).isTrue();
        // Same instance, same status, NO new event raised — dunning silence.
        assertThat(second.getValue()).isSameAs(firstTransition);
        assertThat(second.getValue().getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(second.getValue().getDomainEvents()).isEmpty();
    }

    @Test
    void should_reject_mark_payment_action_required_when_cancelled() {
        Subscription cancelled = monthlySubscription().markFullyCancelled().getValue();

        Result<Subscription> result =
                cancelled.markPaymentActionRequired("https://invoice.stripe.com/x");

        assertThat(result.isFailure()).isTrue();
    }

    private Subscription monthlySubscription() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        return Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                "XDR Pro", "XDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(299.99), "EUR"),
                start, end, end, null, null
        );
    }
}
