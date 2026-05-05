package com.cyna.modules.subscription.infrastructure.scheduling;

import com.cyna.modules.subscription.application.command.reminder.ProcessSubscriptionAutoRenewRemindersCommand;
import com.cyna.shared.application.Mediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SubscriptionAutoRenewReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAutoRenewReminderScheduler.class);

    private final Mediator mediator;

    public SubscriptionAutoRenewReminderScheduler(Mediator mediator) {
        this.mediator = mediator;
    }

    @Scheduled(cron = "${app.subscription.auto-renew-reminder.cron:0 0 8 * * *}", zone = "UTC")
    public void run() {
        var result = mediator.send(new ProcessSubscriptionAutoRenewRemindersCommand(Instant.now()));
        result.fold(
                processed -> {
                    log.info("Subscription auto-renew reminder job processed {} subscriptions", processed);
                    return null;
                },
                error -> {
                    log.error("Subscription auto-renew reminder job failed: {}", error);
                    return null;
                }
        );
    }
}
