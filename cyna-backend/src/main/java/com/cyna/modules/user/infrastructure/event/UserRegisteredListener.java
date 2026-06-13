package com.cyna.modules.user.infrastructure.event;

import com.cyna.modules.user.domain.event.UserRegistered;
import com.cyna.shared.application.notification.MailService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegisteredListener {

    private final MailService mailService;

    public UserRegisteredListener(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * Welcome email is a side effect that must not fire if the registration
     * transaction rolls back — hence AFTER_COMMIT. {@code UserRegistered} is
     * published inside the registration transaction, so the listener runs once
     * the account is durably committed.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserRegistered event) {
        mailService.sendWelcomeEmail(
            event.email(),
            event.firstName(),
            event.lang()
        );
    }
}
