package com.cyna.modules.user.infrastructure.event;

import com.cyna.modules.user.domain.event.UserPasswordChanged;
import com.cyna.shared.application.notification.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fires the security alert email after a password change is committed.
 * Uses {@code AFTER_COMMIT} so an aborted transaction never sends a false alert.
 */
@Component
public class UserPasswordChangedListener {

    private static final Logger log = LoggerFactory.getLogger(UserPasswordChangedListener.class);

    private final MailService mailService;

    public UserPasswordChangedListener(MailService mailService) {
        this.mailService = mailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserPasswordChanged event) {
        try {
            mailService.sendPasswordChangedAlert(event.email(), event.firstName(), event.lang());
        } catch (Exception e) {
            log.error("[password-changed-alert] mail dispatch failed userId={}", event.userId(), e);
        }
    }
}
