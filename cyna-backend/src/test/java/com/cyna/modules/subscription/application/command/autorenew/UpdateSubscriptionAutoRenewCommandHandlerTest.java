package com.cyna.modules.subscription.application.command.autorenew;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSubscriptionAutoRenewCommandHandlerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private UpdateSubscriptionAutoRenewCommandHandler handler;

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
        handler = new UpdateSubscriptionAutoRenewCommandHandler(subscriptionRepository, transactionRunner);
    }

    @Test
    void should_update_auto_renew_successfully() {
        Subscription subscription = createActiveSubscription();
        UpdateSubscriptionAutoRenewCommand command = new UpdateSubscriptionAutoRenewCommand(
                subscription.getId(),
                subscription.getUserId(),
                false
        );

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().autoRenew()).isFalse();
        verify(subscriptionRepository).save(any(Subscription.class));
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
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void should_fail_when_subscription_is_not_active() {
        Subscription cancelled = createActiveSubscription().cancelAtPeriodEnd().getValue();
        UpdateSubscriptionAutoRenewCommand command = new UpdateSubscriptionAutoRenewCommand(
                cancelled.getId(),
                cancelled.getUserId(),
                false
        );

        when(subscriptionRepository.findById(cancelled.getId())).thenReturn(Optional.of(cancelled));

        Result<SubscriptionReadModel> result = handler.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("active");
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
                nextBilling
        );
    }
}
