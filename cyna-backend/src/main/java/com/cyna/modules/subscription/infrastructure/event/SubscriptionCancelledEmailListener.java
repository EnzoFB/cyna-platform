package com.cyna.modules.subscription.infrastructure.event;

import com.cyna.modules.subscription.domain.event.SubscriptionCancelled;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.modules.user.application.api.UserNotificationView;
import com.cyna.modules.user.application.api.UserQueryApi;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.application.notification.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Confirms a cancellation to the customer once it is committed. Fires on
 * {@code AFTER_COMMIT}. DB reads inside a short transaction; the Brevo HTTP
 * call is made outside it (no DB connection held during external I/O — same
 * pattern as {@code OrderPaidEmailListener}). Best-effort.
 */
@Component
public class SubscriptionCancelledEmailListener {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionCancelledEmailListener.class);

    private final SubscriptionRepository subscriptionRepository;
    private final UserQueryApi userQueryApi;
    private final MailService mailService;
    private final TransactionRunner transactionRunner;

    public SubscriptionCancelledEmailListener(SubscriptionRepository subscriptionRepository,
                                              UserQueryApi userQueryApi,
                                              MailService mailService,
                                              TransactionRunner transactionRunner) {
        this.subscriptionRepository = subscriptionRepository;
        this.userQueryApi = userQueryApi;
        this.mailService = mailService;
        this.transactionRunner = transactionRunner;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SubscriptionCancelled event) {
        try {
            MailPayload payload = transactionRunner.runReturning(() -> buildPayload(event));
            if (payload == null) {
                return;
            }
            mailService.sendSubscriptionCancellationConfirmation(
                    payload.email(), payload.firstName(), payload.productName(), payload.lang());
        } catch (Exception e) {
            log.error("[subscription-cancelled-mail] dispatch failed subId={} userId={}",
                    event.subscriptionId(), event.userId(), e);
        }
    }

    private MailPayload buildPayload(SubscriptionCancelled event) {
        Subscription subscription = subscriptionRepository.findById(event.subscriptionId()).orElse(null);
        if (subscription == null) {
            log.error("[subscription-cancelled-mail] subscription not found id={}", event.subscriptionId());
            return null;
        }
        UserNotificationView user = userQueryApi.findUserForNotification(event.userId()).orElse(null);
        if (user == null) {
            log.error("[subscription-cancelled-mail] user not found userId={} subId={}",
                    event.userId(), event.subscriptionId());
            return null;
        }
        return new MailPayload(user.email(), user.firstName(), subscription.getProductName(), user.lang());
    }

    private record MailPayload(String email, String firstName, String productName, String lang) {}
}
