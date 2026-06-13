package com.cyna.modules.subscription.application.command.autorenew;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.domain.event.SubscriptionRenewalPreferenceChanged;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSubscriptionAutoRenewCommandHandlerTest {

    private static final String STRIPE_SUB_ID = "sub_test";

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private UpdateSubscriptionAutoRenewCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateSubscriptionAutoRenewCommandHandler(subscriptionRepository, eventPublisher);
    }

    @Test
    void should_publish_renewal_preference_change_when_disabling_auto_renew() {
        Subscription subscription = createActiveSubscription();
        UpdateSubscriptionAutoRenewCommand command = new UpdateSubscriptionAutoRenewCommand(
                subscription.getId(),
                subscription.getUserId(),
                false
        );

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        // Projected state is returned for instant UI feedback. DB is NOT written —
        // the customer.subscription.updated webhook handler is the only path that
        // mutates subscription_schema.subscriptions for this flow.
        assertThat(result.getValue().autoRenew()).isFalse();

        var captor = ArgumentCaptor.forClass(SubscriptionRenewalPreferenceChanged.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().stripeSubscriptionId()).isEqualTo(STRIPE_SUB_ID);
        assertThat(captor.getValue().autoRenew()).isFalse();
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void should_publish_renewal_preference_change_when_re_enabling_auto_renew() {
        Subscription subscription = createActiveSubscription().updateAutoRenew(false).getValue();
        UpdateSubscriptionAutoRenewCommand command = new UpdateSubscriptionAutoRenewCommand(
                subscription.getId(),
                subscription.getUserId(),
                true
        );

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().autoRenew()).isTrue();

        var captor = ArgumentCaptor.forClass(SubscriptionRenewalPreferenceChanged.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().stripeSubscriptionId()).isEqualTo(STRIPE_SUB_ID);
        assertThat(captor.getValue().autoRenew()).isTrue();
        verify(subscriptionRepository, never()).save(any());
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
        verify(eventPublisher, never()).publish(any());
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
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void should_not_publish_when_toggle_is_a_noop() {
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
        verify(eventPublisher, never()).publish(any());
    }

    private Subscription createActiveSubscription() {
        Instant start = Instant.now();
        Instant end = start.plus(365, ChronoUnit.DAYS);
        Instant nextBilling = start.plus(365, ChronoUnit.DAYS);

        return Subscription.createActive(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
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
