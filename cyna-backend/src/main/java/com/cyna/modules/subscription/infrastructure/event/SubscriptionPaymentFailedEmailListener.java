package com.cyna.modules.subscription.infrastructure.event;

import com.cyna.modules.subscription.domain.event.SubscriptionPaymentFailed;
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
 * Warns the customer when a renewal payment failed and the subscription went
 * PAST_DUE. Fires on {@code AFTER_COMMIT}. The DB reads happen inside a short
 * transaction; the Brevo HTTP call is made <em>outside</em> it so no DB
 * connection is held during external I/O — same pattern as
 * {@code OrderPaidEmailListener}. Best-effort: a delivery failure is logged,
 * never propagated.
 */
@Component
public class SubscriptionPaymentFailedEmailListener {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionPaymentFailedEmailListener.class);

    private final SubscriptionRepository subscriptionRepository;
    private final UserQueryApi userQueryApi;
    private final MailService mailService;
    private final TransactionRunner transactionRunner;

    public SubscriptionPaymentFailedEmailListener(SubscriptionRepository subscriptionRepository,
                                                  UserQueryApi userQueryApi,
                                                  MailService mailService,
                                                  TransactionRunner transactionRunner) {
        this.subscriptionRepository = subscriptionRepository;
        this.userQueryApi = userQueryApi;
        this.mailService = mailService;
        this.transactionRunner = transactionRunner;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SubscriptionPaymentFailed event) {
        try {
            MailPayload payload = transactionRunner.runReturning(() -> buildPayload(event));
            if (payload == null) {
                return;
            }
            mailService.sendSubscriptionPaymentFailed(
                    payload.email(), payload.firstName(), payload.productName(), payload.lang());
        } catch (Exception e) {
            log.error("[subscription-pastdue-mail] dispatch failed subId={} userId={}",
                    event.subscriptionId(), event.userId(), e);
        }
    }

    private MailPayload buildPayload(SubscriptionPaymentFailed event) {
        Subscription subscription = subscriptionRepository.findById(event.subscriptionId()).orElse(null);
        if (subscription == null) {
            log.error("[subscription-pastdue-mail] subscription not found id={}", event.subscriptionId());
            return null;
        }
        UserNotificationView user = userQueryApi.findUserForNotification(event.userId()).orElse(null);
        if (user == null) {
            log.error("[subscription-pastdue-mail] user not found userId={} subId={}",
                    event.userId(), event.subscriptionId());
            return null;
        }
        return new MailPayload(user.email(), user.firstName(), subscription.getProductName(), user.lang());
    }

    private record MailPayload(String email, String firstName, String productName, String lang) {}
}
