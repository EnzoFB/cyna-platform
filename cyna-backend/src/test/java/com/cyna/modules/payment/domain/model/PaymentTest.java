package com.cyna.modules.payment.domain.model;

import com.cyna.modules.payment.domain.event.PaymentFailed;
import com.cyna.modules.payment.domain.event.PaymentInitiated;
import com.cyna.modules.payment.domain.event.PaymentSucceeded;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    private static final Money AMOUNT = Money.of(BigDecimal.valueOf(199.99), "EUR");

    @Test
    void should_create_pending_payment_and_raise_initiated_event() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Payment payment = Payment.create(id, orderId, userId, AMOUNT);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getStripePaymentIntentId()).isNull();
        assertThat(payment.getStripeSubscriptionId()).isNull();
        assertThat(payment.getStripeScheduleId()).isNull();
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().get(0)).isInstanceOf(PaymentInitiated.class);
    }

    @Test
    void should_assign_stripe_subscription_and_keep_status_pending() {
        Payment payment = Payment.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AMOUNT
        );

        Payment withStripe = payment.assignStripeSubscription(
                "pi_123", "pi_123_secret_abc", "sub_123", "sch_123"
        );

        assertThat(withStripe.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(withStripe.getStripePaymentIntentId()).isEqualTo("pi_123");
        assertThat(withStripe.getStripeClientSecret()).isEqualTo("pi_123_secret_abc");
        assertThat(withStripe.getStripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(withStripe.getStripeScheduleId()).isEqualTo("sch_123");
    }

    @Test
    void should_mark_succeeded_and_carry_stripe_subscription_id_in_event() {
        Payment payment = Payment.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AMOUNT
        ).assignStripeSubscription("pi_123", "pi_secret", "sub_123", "sch_123");
        payment.clearDomainEvents();

        Result<Payment> result = payment.markSucceeded();

        assertThat(result.isSuccess()).isTrue();
        Payment succeeded = result.getValue();
        assertThat(succeeded.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(succeeded.getStripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(succeeded.getDomainEvents()).hasSize(1);

        var event = (PaymentSucceeded) succeeded.getDomainEvents().get(0);
        assertThat(event.stripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(event.orderId()).isEqualTo(payment.getOrderId());
    }

    @Test
    void should_mark_failed_and_keep_stripe_references() {
        Payment payment = Payment.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AMOUNT
        ).assignStripeSubscription("pi_123", "pi_secret", "sub_123", "sch_123");

        Result<Payment> result = payment.markFailed();

        assertThat(result.isSuccess()).isTrue();
        Payment failed = result.getValue();
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.getStripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(failed.getDomainEvents()).anyMatch(e -> e instanceof PaymentFailed);
    }

    @Test
    void should_reject_mark_succeeded_when_already_succeeded() {
        Payment payment = Payment.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AMOUNT
        ).assignStripeSubscription("pi_123", "pi_secret", "sub_123", "sch_123");
        Payment succeeded = payment.markSucceeded().getValue();

        Result<Payment> result = succeeded.markSucceeded();

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void should_reject_mark_failed_after_success() {
        Payment payment = Payment.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AMOUNT
        ).assignStripeSubscription("pi_123", "pi_secret", "sub_123", "sch_123");
        Payment succeeded = payment.markSucceeded().getValue();

        Result<Payment> result = succeeded.markFailed();

        assertThat(result.isFailure()).isTrue();
    }
}
