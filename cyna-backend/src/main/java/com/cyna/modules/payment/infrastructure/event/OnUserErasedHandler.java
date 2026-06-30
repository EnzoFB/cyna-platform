package com.cyna.modules.payment.infrastructure.event;

import com.cyna.modules.payment.domain.repository.PaymentConsentLogRepository;
import com.cyna.modules.user.application.api.event.UserErasedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Purges the payment module's data keyed by a hard-deleted user — today only
 * the {@code payment_consent_log}.
 *
 * <p><b>Why this exists.</b> While every module shares one database, that purge
 * is done by the cross-schema {@code payment_consent_log → users}
 * {@code ON DELETE CASCADE} foreign key (into the user schema): deleting the
 * user row removes the consent logs in the same commit. That FK cannot survive
 * a split into separate databases, so this handler is its split-ready
 * replacement — payment cleans up its own data in reaction to the
 * {@link UserErasedIntegrationEvent} published by the user module.
 *
 * <p><b>Belt-and-suspenders, on purpose.</b> Both mechanisms run today: the FK
 * cascade is still authoritative, and this handler is idempotent (deleting rows
 * the cascade already removed is a harmless no-op). That is what makes the
 * migration safe and incremental — add the event path, verify it, and only then
 * drop the FK at the actual extraction; nothing in this module changes at that
 * point.
 *
 * <p>Reacts {@code AFTER_COMMIT} so the purge happens only once the user erasure
 * has truly committed — modelling the post-split semantics where payment reacts
 * to a fact the user service has already durably recorded. The repository delete
 * opens its own transaction, which is required since an after-commit listener
 * runs with no ambient transaction.
 */
@Component
public class OnUserErasedHandler {

    private static final Logger log = LoggerFactory.getLogger(OnUserErasedHandler.class);

    private final PaymentConsentLogRepository consentLogRepository;

    public OnUserErasedHandler(PaymentConsentLogRepository consentLogRepository) {
        this.consentLogRepository = consentLogRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserErasedIntegrationEvent event) {
        consentLogRepository.deleteAllByUserId(event.userId());
        log.info("RGPD erasure: purged payment consent logs for hard-deleted user {}", event.userId());
    }
}
