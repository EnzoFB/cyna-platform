package com.cyna.modules.subscription.application.command.cancel;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.model.SubscriptionStatus;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelSubscriptionCommandHandlerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private CancelSubscriptionCommandHandler handler;

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
        handler = new CancelSubscriptionCommandHandler(subscriptionRepository, transactionRunner, eventPublisher);
    }

    @Test
    void should_cancel_subscription_successfully() {
        Subscription subscription = createActiveSubscription();
        CancelSubscriptionCommand command = new CancelSubscriptionCommand(subscription.getId(), subscription.getUserId());

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().status()).isEqualTo("CANCELLED");
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(eventPublisher).publishAll(anyList());
    }

    @Test
    void should_fail_when_subscription_not_found() {
        UUID subscriptionId = UUID.randomUUID();
        CancelSubscriptionCommand command = new CancelSubscriptionCommand(subscriptionId, UUID.randomUUID());

        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("Subscription not found");
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(eventPublisher, never()).publishAll(anyList());
    }

    @Test
    void should_fail_when_user_has_no_access() {
        Subscription subscription = createActiveSubscription();
        CancelSubscriptionCommand command = new CancelSubscriptionCommand(subscription.getId(), UUID.randomUUID());

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Access denied");
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(eventPublisher, never()).publishAll(anyList());
    }

    @Test
    void should_fail_when_subscription_is_not_active() {
        Subscription paused = createPausedSubscription();
        CancelSubscriptionCommand command = new CancelSubscriptionCommand(paused.getId(), paused.getUserId());

        when(subscriptionRepository.findById(paused.getId())).thenReturn(Optional.of(paused));

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("active");
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(eventPublisher, never()).publishAll(anyList());
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
                nextBilling
        );
    }

    private Subscription createPausedSubscription() {
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();

        return Subscription.reconstitute(
                UUID.randomUUID(),
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SOC Premium",
                "SOC",
                BillingCycle.MONTHLY,
                SubscriptionStatus.PAUSED,
                1,
                Money.of(BigDecimal.valueOf(299.99), "EUR"),
                start,
                end,
                end,
                null,
                true,
                null,
                now,
                now
        );
    }
}
