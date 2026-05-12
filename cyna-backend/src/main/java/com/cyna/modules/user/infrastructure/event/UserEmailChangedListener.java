package com.cyna.modules.user.infrastructure.event;

import com.cyna.modules.user.domain.event.UserEmailChanged;
import com.cyna.shared.application.notification.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fires the security alert email to the PREVIOUS address after an email change
 * is committed — gives the legitimate owner a chance to react if the account
 * was hijacked. The new address already received a confirmation link earlier
 * in the flow, so we do not email it again here.
 */
@Component
public class UserEmailChangedListener {

    private static final Logger log = LoggerFactory.getLogger(UserEmailChangedListener.class);

    private final MailService mailService;

    public UserEmailChangedListener(MailService mailService) {
        this.mailService = mailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserEmailChanged event) {
        try {
            mailService.sendEmailChangedAlert(
                    event.oldEmail(), event.newEmail(), event.firstName(), event.lang()
            );
        } catch (Exception e) {
            log.error("[email-changed-alert] mail dispatch failed userId={}", event.userId(), e);
        }
    }
}
