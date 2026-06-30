package com.cyna.modules.subscription.application.command.sync;

import com.cyna.modules.subscription.domain.event.SubscriptionCancelled;
import com.cyna.modules.subscription.domain.event.SubscriptionRenewed;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.model.SubscriptionStatus;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.DomainEvent;
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
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
class SyncSubscriptionsFromStripeCommandHandlerTest {

    private static final String STRIPE_SUB_ID = "sub_sync";

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private SyncSubscriptionsFromStripeCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) {
            action.run();
        }

        @Override
        public <T> T runReturning(Supplier<T> action) {
            return action.get();
        }
    };

    @BeforeEach
    void setUp() {
        handler = new SyncSubscriptionsFromStripeCommandHandler(
                subscriptionRepository, eventPublisher, transactionRunner);
    }

    @Test
    void should_disable_auto_renew_locally_when_cancel_at_period_end_arrives_true() {
        Subscription sub = activeSubscription(); // autoRenew=true
        when(subscriptionRepository.findAllByStripeSubscriptionId(STRIPE_SUB_ID))
                .thenReturn(List.of(sub));

        Result<Void> result = handler.handle(new SyncSubscriptionsFromStripeCommand(
                STRIPE_SUB_ID, "active", Boolean.TRUE, sub.getEndAt(), null
        ));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(saved.capture());
        assertThat(saved.getValue().isAutoRenew()).isFalse();
        assertThat(saved.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void should_transition_to_cancelled_when_stripe_status_canceled() {
        Subscription sub = activeSubscription();
        when(subscriptionRepository.findAllByStripeSubscriptionId(STRIPE_SUB_ID))
                .thenReturn(List.of(sub));

        // Snapshot events at call-time. AggregateRoot.getDomainEvents() returns a
        // live unmodifiable view of the internal list, and the handler clears it
        // immediately after publishAll() — so a deferred capture (ArgumentCaptor
        // or argThat) would read it empty.
        List<DomainEvent> publishedEvents = new ArrayList<>();
        doAnswer(inv -> {
            publishedEvents.addAll(inv.getArgument(0));
            return null;
        }).when(eventPublisher).publishAll(any());

        Result<Void> result = handler.handle(new SyncSubscriptionsFromStripeCommand(
                STRIPE_SUB_ID, "canceled", null, sub.getEndAt(), Instant.now()
        ));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(publishedEvents).anyMatch(e -> e instanceof SubscriptionCancelled);
    }

    @Test
    void should_transition_to_past_due_when_stripe_status_past_due() {
        Subscription sub = activeSubscription();
        when(subscriptionRepository.findAllByStripeSubscriptionId(STRIPE_SUB_ID))
                .thenReturn(List.of(sub));

        Result<Void> result = handler.handle(new SyncSubscriptionsFromStripeCommand(
                STRIPE_SUB_ID, "past_due", Boolean.FALSE, sub.getEndAt(), null
        ));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void should_recover_from_past_due_when_stripe_status_returns_active() {
        Subscription sub = activeSubscription().markPastDue().getValue();
        when(subscriptionRepository.findAllByStripeSubscriptionId(STRIPE_SUB_ID))
                .thenReturn(List.of(sub));

        Result<Void> result = handler.handle(new SyncSubscriptionsFromStripeCommand(
                STRIPE_SUB_ID, "active", Boolean.FALSE, sub.getEndAt(), null
        ));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void should_extend_periods_when_current_period_end_moves_forward() {
        Subscription sub = activeSubscription();
        Instant newPeriodEnd = sub.getEndAt().plus(30, ChronoUnit.DAYS);
        when(subscriptionRepository.findAllByStripeSubscriptionId(STRIPE_SUB_ID))
                .thenReturn(List.of(sub));

        List<DomainEvent> publishedEvents = new ArrayList<>();
        doAnswer(inv -> {
            publishedEvents.addAll(inv.getArgument(0));
            return null;
        }).when(eventPublisher).publishAll(any());

        Result<Void> result = handler.handle(new SyncSubscriptionsFromStripeCommand(
                STRIPE_SUB_ID, "active", Boolean.FALSE, newPeriodEnd, null
        ));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(saved.capture());
        assertThat(saved.getValue().getEndAt()).isEqualTo(newPeriodEnd);
        assertThat(publishedEvents).anyMatch(e -> e instanceof SubscriptionRenewed);
    }

    @Test
    void should_skip_save_when_state_already_matches_stripe() {
        Subscription sub = activeSubscription(); // autoRenew=true, ACTIVE
        when(subscriptionRepository.findAllByStripeSubscriptionId(STRIPE_SUB_ID))
                .thenReturn(List.of(sub));

        Result<Void> result = handler.handle(new SyncSubscriptionsFromStripeCommand(
                STRIPE_SUB_ID, "active", Boolean.FALSE, sub.getEndAt(), null
        ));

        assertThat(result.isSuccess()).isTrue();
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void should_no_op_when_no_local_subscription_matches() {
        when(subscriptionRepository.findAllByStripeSubscriptionId("sub_unknown"))
                .thenReturn(List.of());

        Result<Void> result = handler.handle(new SyncSubscriptionsFromStripeCommand(
                "sub_unknown", "active", Boolean.FALSE, Instant.now(), null
        ));

        assertThat(result.isSuccess()).isTrue();
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    private Subscription activeSubscription() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        return Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                "EDR Advanced", "EDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(99.99), "EUR"),
                start, end, end, STRIPE_SUB_ID, null
        );
    }
}
