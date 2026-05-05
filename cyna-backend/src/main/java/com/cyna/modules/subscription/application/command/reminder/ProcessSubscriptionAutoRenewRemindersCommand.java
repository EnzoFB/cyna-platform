package com.cyna.modules.subscription.application.command.reminder;

import com.cyna.shared.application.Command;

import java.time.Instant;

public record ProcessSubscriptionAutoRenewRemindersCommand(
        Instant now
) implements Command<Integer> {
}
