package com.cyna.modules.subscription.application.command.reminder;

import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import com.cyna.shared.infrastructure.notification.MailService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class ProcessSubscriptionAutoRenewRemindersCommandHandler implements CommandHandler<ProcessSubscriptionAutoRenewRemindersCommand, Integer> {

    private static final DateTimeFormatter RENEWAL_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.FRANCE);

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final TransactionRunner transactionRunner;

    public ProcessSubscriptionAutoRenewRemindersCommandHandler(SubscriptionRepository subscriptionRepository,
                                                               UserRepository userRepository,
                                                               MailService mailService,
                                                               TransactionRunner transactionRunner) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Integer> handle(ProcessSubscriptionAutoRenewRemindersCommand command) {
        Instant now = command.now() != null ? command.now() : Instant.now();
        LocalDate targetDate = LocalDate.ofInstant(now, ZoneOffset.UTC).plusMonths(2);
        Instant fromInclusive = targetDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = targetDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Subscription> dueSubscriptions = subscriptionRepository.findActiveAutoRenewDueForNotice(
                fromInclusive,
                toExclusive
        );

        int processed = 0;
        for (Subscription subscription : dueSubscriptions) {
            var userOpt = userRepository.findById(subscription.getUserId());
            if (userOpt.isEmpty()) {
                continue;
            }

            String renewalDate = RENEWAL_DATE_FORMATTER.format(
                    LocalDate.ofInstant(subscription.getEndAt(), ZoneOffset.UTC)
            );

            mailService.sendSubscriptionAutoRenewReminder(
                    userOpt.get().getEmail().value(),
                    userOpt.get().getFirstName(),
                    subscription.getProductName(),
                    renewalDate,
                    "fr"
            );

            Result<Subscription> marked = subscription.markAutoRenewNoticeSent(now);
            if (marked.isSuccess()) {
                Subscription updated = marked.getValue();
                transactionRunner.run(() -> subscriptionRepository.save(updated));
                processed++;
            }
        }

        return Result.success(processed);
    }
}
