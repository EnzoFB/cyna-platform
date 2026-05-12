package com.cyna.modules.subscription.application.command.autorenew;

import com.cyna.modules.payment.application.api.PaymentCommandApi;
import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSubscriptionAutoRenewCommandHandlerTest {

    private static final String STRIPE_SUB_ID = "sub_test";

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentCommandApi paymentCommandApi;

    private UpdateSubscriptionAutoRenewCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateSubscriptionAutoRenewCommandHandler(subscriptionRepository, paymentCommandApi);
    }

    @Test
    void should_push_cancel_at_period_end_true_to_stripe_when_disabling_auto_renew() {
        Subscription subscription = createActiveSubscription();
        UpdateSubscriptionAutoRenewCommand command = new UpdateSubscriptionAutoRenewCommand(
                subscription.getId(),
                subscription.getUserId(),
                false
        );

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentCommandApi.setStripeSubscriptionCancelAtPeriodEnd(STRIPE_SUB_ID, true))
                .thenReturn(Result.success());

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        // Projected state is returned for instant UI feedback. DB is NOT written —
        // the customer.subscription.updated webhook handler is the only path that
        // mutates subscription_schema.subscriptions for this flow.
        assertThat(result.getValue().autoRenew()).isFalse();
        verify(paymentCommandApi).setStripeSubscriptionCancelAtPeriodEnd(STRIPE_SUB_ID, true);
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void should_push_cancel_at_period_end_false_to_stripe_when_re_enabling_auto_renew() {
        Subscription subscription = createActiveSubscription().updateAutoRenew(false).getValue();
        UpdateSubscriptionAutoRenewCommand command = new UpdateSubscriptionAutoRenewCommand(
                subscription.getId(),
                subscription.getUserId(),
                true
        );

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentCommandApi.setStripeSubscriptionCancelAtPeriodEnd(STRIPE_SUB_ID, false))
                .thenReturn(Result.success());

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().autoRenew()).isTrue();
        verify(paymentCommandApi).setStripeSubscriptionCancelAtPeriodEnd(STRIPE_SUB_ID, false);
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void should_fail_when_subscription_not_found() {
        UUID subscriptionId = UUID.randomUUID();
        UpdateSubscriptionAutoRenewCommand command = new UpdateSubscriptionAutoRenewCommand(
                subscriptionId,
                UUID.randomUUID(),
                false
        );

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Subscription not found");
        verify(paymentCommandApi, never()).setStripeSubscriptionCancelAtPeriodEnd(any(), anyBoolean());
    }

    @Test
    void should_fail_when_subscription_is_terminated() {
        Subscription cancelled = createActiveSubscription().markFullyCancelled().getValue();
        UpdateSubscriptionAutoRenewCommand command = new UpdateSubscriptionAutoRenewCommand(
                cancelled.getId(),
                cancelled.getUserId(),
                false
        );

        when(subscriptionRepository.findById(cancelled.getId())).thenReturn(Optional.of(cancelled));

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        verify(paymentCommandApi, never()).setStripeSubscriptionCancelAtPeriodEnd(any(), anyBoolean());
    }

    @Test
    void should_skip_stripe_call_when_toggle_is_a_noop() {
        Subscription subscription = createActiveSubscription(); // autoRenew already true
        UpdateSubscriptionAutoRenewCommand command = new UpdateSubscriptionAutoRenewCommand(
                subscription.getId(),
                subscription.getUserId(),
                true
        );
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().autoRenew()).isTrue();
        verify(paymentCommandApi, never()).setStripeSubscriptionCancelAtPeriodEnd(any(), anyBoolean());
    }

    @Test
    void should_propagate_stripe_failure() {
        Subscription subscription = createActiveSubscription();
        UpdateSubscriptionAutoRenewCommand command = new UpdateSubscriptionAutoRenewCommand(
                subscription.getId(),
                subscription.getUserId(),
                false
        );

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentCommandApi.setStripeSubscriptionCancelAtPeriodEnd(eq(STRIPE_SUB_ID), eq(true)))
                .thenReturn(Result.failure("STRIPE_ERROR: rate limited"));

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("STRIPE_ERROR");
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    private Subscription createActiveSubscription() {
        Instant start = Instant.now();
        Instant end = start.plus(365, ChronoUnit.DAYS);
        Instant nextBilling = start.plus(365, ChronoUnit.DAYS);

        return Subscription.createActive(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "XDR Enterprise",
                "XDR",
                BillingCycle.ANNUAL,
                1,
                Money.of(BigDecimal.valueOf(1499.99), "EUR"),
                start,
                end,
                nextBilling,
                STRIPE_SUB_ID,
                null
        );
    }
}
