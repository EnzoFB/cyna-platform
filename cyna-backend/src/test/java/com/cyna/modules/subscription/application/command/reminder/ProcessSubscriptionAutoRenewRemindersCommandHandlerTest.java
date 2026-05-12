package com.cyna.modules.subscription.application.command.reminder;

import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.HashedPassword;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import com.cyna.shared.application.notification.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessSubscriptionAutoRenewRemindersCommandHandlerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    private ProcessSubscriptionAutoRenewRemindersCommandHandler handler;

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
        handler = new ProcessSubscriptionAutoRenewRemindersCommandHandler(
                subscriptionRepository,
                userRepository,
                mailService,
                transactionRunner
        );
    }

    @Test
    void should_send_notice_and_mark_subscription() {
        Subscription subscription = createAnnualActiveSubscription();
        User user = User.register(
                Email.of("customer@example.com"),
                HashedPassword.of("hashed"),
                "Alice",
                "Martin",
                "fr"
        );

        when(subscriptionRepository.findActiveAutoRenewDueForNotice(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(subscription));
        when(userRepository.findById(subscription.getUserId()))
                .thenReturn(Optional.of(user));

        Result<Integer> result = handler.handle(new ProcessSubscriptionAutoRenewRemindersCommand(Instant.now()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(1);
        verify(mailService).sendSubscriptionAutoRenewReminder(
                any(String.class),
                any(String.class),
                any(String.class),
                any(String.class),
                any(String.class)
        );
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void should_skip_when_user_not_found() {
        Subscription subscription = createAnnualActiveSubscription();

        when(subscriptionRepository.findActiveAutoRenewDueForNotice(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(subscription));
        when(userRepository.findById(subscription.getUserId()))
                .thenReturn(Optional.empty());

        Result<Integer> result = handler.handle(new ProcessSubscriptionAutoRenewRemindersCommand(Instant.now()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(0);
        verify(mailService, never()).sendSubscriptionAutoRenewReminder(
                any(String.class),
                any(String.class),
                any(String.class),
                any(String.class),
                any(String.class)
        );
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    private Subscription createAnnualActiveSubscription() {
        Instant start = Instant.now();
        Instant end = start.plus(365, ChronoUnit.DAYS);
        Instant nextBilling = end;

        return Subscription.createActive(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SOC Enterprise",
                "SOC",
                BillingCycle.ANNUAL,
                1,
                Money.of(BigDecimal.valueOf(1299.99), "EUR"),
                start,
                end,
                nextBilling,
                null,
                null
        );
    }
}
