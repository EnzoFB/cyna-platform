package com.cyna.modules.user.infrastructure.event;

import com.cyna.modules.user.domain.event.SuspiciousAuthActivityDetected;
import com.cyna.shared.application.notification.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifies the user after refresh-token reuse was detected. The corresponding
 * transaction has already revoked every active session; this listener simply
 * informs the user once the rollback-or-commit decision has settled.
 */
@Component
public class SuspiciousAuthActivityListener {

    private static final Logger log = LoggerFactory.getLogger(SuspiciousAuthActivityListener.class);

    private final MailService mailService;

    public SuspiciousAuthActivityListener(MailService mailService) {
        this.mailService = mailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SuspiciousAuthActivityDetected event) {
        try {
            mailService.sendSuspiciousActivityAlert(event.email(), event.firstName(), event.lang());
        } catch (Exception e) {
            log.error(
                    "[suspicious-activity-alert] mail dispatch failed userId={} reason={}",
                    event.userId(), event.reason(), e
            );
        }
    }
}
