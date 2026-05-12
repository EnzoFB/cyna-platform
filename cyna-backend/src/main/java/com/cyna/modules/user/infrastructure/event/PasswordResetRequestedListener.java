package com.cyna.modules.user.infrastructure.event;

import com.cyna.modules.user.domain.event.PasswordResetRequested;
import com.cyna.shared.application.notification.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Mails the reset link after the request transaction commits. Using
 * {@code AFTER_COMMIT} guarantees we never email a token that was rolled back
 * — e.g. if persistence failed mid-flight, no link reaches the user.
 */
@Component
public class PasswordResetRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetRequestedListener.class);

    private final MailService mailService;

    public PasswordResetRequestedListener(MailService mailService) {
        this.mailService = mailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PasswordResetRequested event) {
        try {
            mailService.sendPasswordResetEmail(
                    event.email(), event.firstName(), event.rawToken(), event.lang()
            );
        } catch (Exception e) {
            log.error("[password-reset-request] mail dispatch failed userId={}", event.userId(), e);
        }
    }
}
