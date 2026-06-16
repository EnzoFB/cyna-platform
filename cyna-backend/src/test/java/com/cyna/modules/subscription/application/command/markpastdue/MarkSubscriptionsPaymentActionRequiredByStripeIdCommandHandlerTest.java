package com.cyna.modules.subscription.application.command.markpastdue;

import com.cyna.modules.subscription.domain.event.SubscriptionPaymentActionRequired;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.model.SubscriptionStatus;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.BillingCycle;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkSubscriptionsPaymentActionRequiredByStripeIdCommandHandlerTest {

    private static final String STRIPE_SUB_ID = "sub_test_123";
    private static final String HOSTED_INVOICE_URL = "https://invoice.stripe.com/i/test_inv_abc";

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private DomainEventPublisher eventPublisher;

    private MarkSubscriptionsPaymentActionRequiredByStripeIdCommandHandler handler;

    /**
     * Snapshot of every event handed to {@code publishAll(...)} across the
     * test. We can't use {@link ArgumentCaptor} directly: the handler clears
     * the aggregate's events immediately after publishing, and Mockito's
     * captor stores references — by the time we query, the list is empty.
     * Copying eagerly in a {@code doAnswer} preserves the actual payload.
     */
    private final List<DomainEvent> publishedEvents = new ArrayList<>();

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) { action.run(); }

        @Override
        public <T> T runReturning(Supplier<T> action) { return action.get(); }
    };

    @BeforeEach
    void setUp() {
        handler = new MarkSubscriptionsPaymentActionRequiredByStripeIdCommandHandler(
                subscriptionRepository, eventPublisher, transactionRunner);
        // Lenient: the unknown-subscription test exits before publishAll is
        // touched. We don't want Mockito's strict mode to flag that case.
        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<DomainEvent> events = (List<DomainEvent>) inv.getArgument(0);
            publishedEvents.addAll(events);
            return null;
        }).when(eventPublisher).publishAll(any());
    }

    @Test
    void should_be_a_silent_noop_when_unknown_subscription() {
        when(subscriptionRepository.findAllByStripeSubscriptionId("sub_unknown"))
                .thenReturn(List.of());

        Result<Void> result = handler.handle(
                new MarkSubscriptionsPaymentActionRequiredByStripeIdCommand(
                        "sub_unknown", HOSTED_INVOICE_URL));

        assertThat(result.isSuccess()).isTrue();
        verify(subscriptionRepository, never()).save(any());
        verify(eventPublisher, never()).publishAll(any());
    }

    @Test
    void should_transition_active_to_past_due_and_publish_event_on_first_dunning_retry() {
        Subscription active = activeSubscription();
        when(subscriptionRepository.findAllByStripeSubscriptionId(STRIPE_SUB_ID))
                .thenReturn(List.of(active));

        Result<Void> result = handler.handle(
                new MarkSubscriptionsPaymentActionRequiredByStripeIdCommand(
                        STRIPE_SUB_ID, HOSTED_INVOICE_URL));

        assertThat(result.isSuccess()).isTrue();

        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);

        assertThat(publishedEvents).singleElement()
                .isInstanceOf(SubscriptionPaymentActionRequired.class)
                .satisfies(evt -> {
                    var e = (SubscriptionPaymentActionRequired) evt;
                    assertThat(e.subscriptionId()).isEqualTo(active.getId());
                    assertThat(e.hostedInvoiceUrl()).isEqualTo(HOSTED_INVOICE_URL);
                });
    }

    /**
     * Regression guard for the dunning-spam bug. Stripe retries
     * {@code invoice.payment_action_required} every 24-72 h until the customer
     * completes 3DS or Stripe abandons. Each retry has a distinct
     * {@code event.id}, so the webhook dedup table doesn't catch it — the
     * domain-level no-op guard in {@code Subscription.markPaymentActionRequired}
     * is what prevents the customer from receiving the same email N times.
     *
     * <p>The handler may still go through its persist+publish path (it does so
     * unconditionally, mirroring the pattern of the sibling
     * {@code MarkSubscriptionsPastDueByStripeIdCommandHandler}); what we assert
     * is the user-facing guarantee: <b>no {@code SubscriptionPaymentActionRequired}
     * ever reaches the event publisher on a no-op transition</b>. The
     * notification listener therefore never fires and the customer is not
     * re-emailed.
     */
    @Test
    void should_not_re_publish_event_when_subscription_already_past_due() {
        Subscription pastDue = activeSubscription()
                .markPaymentActionRequired(HOSTED_INVOICE_URL)
                .getValue();
        pastDue.clearDomainEvents();

        when(subscriptionRepository.findAllByStripeSubscriptionId(STRIPE_SUB_ID))
                .thenReturn(List.of(pastDue));

        Result<Void> result = handler.handle(
                new MarkSubscriptionsPaymentActionRequiredByStripeIdCommand(
                        STRIPE_SUB_ID, HOSTED_INVOICE_URL));

        assertThat(result.isSuccess()).isTrue();
        assertThat(publishedEvents)
                .as("dunning silence: no payment-action-required event must leak to the publisher")
                .noneMatch(SubscriptionPaymentActionRequired.class::isInstance);
    }

    /**
     * End-to-end repetition: 3 consecutive Stripe dunning retries on the same
     * subscription. Across all 3 invocations exactly ONE
     * {@code SubscriptionPaymentActionRequired} must reach the publisher (the
     * first, on the {@code ACTIVE → PAST_DUE} transition). The customer
     * receives one email, not three.
     */
    @Test
    void publishes_event_exactly_once_across_repeated_dunning_retries() {
        Subscription active = activeSubscription();
        Subscription pastDue = active
                .markPaymentActionRequired(HOSTED_INVOICE_URL)
                .getValue();
        pastDue.clearDomainEvents();

        when(subscriptionRepository.findAllByStripeSubscriptionId(STRIPE_SUB_ID))
                .thenReturn(List.of(active))
                .thenReturn(List.of(pastDue))
                .thenReturn(List.of(pastDue));

        var command = new MarkSubscriptionsPaymentActionRequiredByStripeIdCommand(
                STRIPE_SUB_ID, HOSTED_INVOICE_URL);
        handler.handle(command);
        handler.handle(command);
        handler.handle(command);

        long emitted = publishedEvents.stream()
                .filter(SubscriptionPaymentActionRequired.class::isInstance)
                .count();
        assertThat(emitted)
                .as("3 retries → 1 email only")
                .isEqualTo(1L);
    }

    private Subscription activeSubscription() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        return Subscription.createActive(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                "XDR Pro", "XDR", BillingCycle.MONTHLY, 1,
                Money.of(BigDecimal.valueOf(299.99), "EUR"),
                start, end, end, STRIPE_SUB_ID, null);
    }
}
