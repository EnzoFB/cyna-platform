package com.cyna.modules.notification.infrastructure.event;

import com.cyna.modules.notification.application.eventhandler.OrderNotificationHandler;
import com.cyna.modules.notification.application.eventhandler.SubscriptionNotificationHandler;
import com.cyna.modules.notification.application.eventhandler.UserNotificationHandler;
import com.cyna.modules.order.application.api.event.OrderPaidIntegrationEvent;
import com.cyna.modules.subscription.application.api.event.SubscriptionAutoRenewReminderDueIntegrationEvent;
import com.cyna.modules.subscription.application.api.event.SubscriptionCancelledIntegrationEvent;
import com.cyna.modules.subscription.application.api.event.SubscriptionPaymentActionRequiredIntegrationEvent;
import com.cyna.modules.subscription.application.api.event.SubscriptionPaymentFailedIntegrationEvent;
import com.cyna.modules.subscription.application.api.event.SubscriptionTrialWillEndIntegrationEvent;
import com.cyna.modules.user.application.api.event.EmailChangeRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.EmailVerificationRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.LoginOtpRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.PasswordResetRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.SuspiciousAuthActivityDetectedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserEmailChangedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserEmailVerifiedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserPasswordChangedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The notification module's single subscription point to the rest of the
 * platform. Every method reacts to a <b>published integration event</b> of
 * another module ({@code order.application.api.event},
 * {@code subscription.application.api.event}, {@code user.application.api.event})
 * — the only inbound async coupling allowed by the module-isolation rule. The
 * module never imports another module's {@code domain.event}.
 *
 * <p>Each listener is a thin, {@code AFTER_COMMIT} adapter: it delegates straight
 * to an application handler and never lets a delivery failure bubble back into
 * the committed producer transaction. Because the integration event is published
 * (synchronously) inside that producer transaction, {@code AFTER_COMMIT} still
 * fires only once the producing transaction has committed.
 */
@Component
public class NotificationEventListeners {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListeners.class);

    private final UserNotificationHandler userHandler;
    private final OrderNotificationHandler orderHandler;
    private final SubscriptionNotificationHandler subscriptionHandler;

    public NotificationEventListeners(UserNotificationHandler userHandler,
                                      OrderNotificationHandler orderHandler,
                                      SubscriptionNotificationHandler subscriptionHandler) {
        this.userHandler = userHandler;
        this.orderHandler = orderHandler;
        this.subscriptionHandler = subscriptionHandler;
    }

    // ── User / account & security ─────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredIntegrationEvent event) {
        safely("welcome-email", event.userId(), () -> userHandler.onUserRegistered(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailVerificationRequested(EmailVerificationRequestedIntegrationEvent event) {
        safely("email-verification", event.userId(), () -> userHandler.onEmailVerificationRequested(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserEmailVerified(UserEmailVerifiedIntegrationEvent event) {
        safely("welcome-email", event.userId(), () -> userHandler.onUserEmailVerified(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(UserPasswordChangedIntegrationEvent event) {
        safely("password-changed-alert", event.userId(), () -> userHandler.onPasswordChanged(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailChanged(UserEmailChangedIntegrationEvent event) {
        safely("email-changed-alert", event.userId(), () -> userHandler.onEmailChanged(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedIntegrationEvent event) {
        safely("password-reset-request", event.userId(), () -> userHandler.onPasswordResetRequested(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSuspiciousActivity(SuspiciousAuthActivityDetectedIntegrationEvent event) {
        safely("suspicious-activity-alert", event.userId(), () -> userHandler.onSuspiciousActivity(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoginOtpRequested(LoginOtpRequestedIntegrationEvent event) {
        safely("login-otp-mail", event.email(), () -> userHandler.onLoginOtpRequested(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailChangeRequested(EmailChangeRequestedIntegrationEvent event) {
        safely("email-change-confirmation", event.userId(), () -> userHandler.onEmailChangeRequested(event));
    }

    // ── Order ─────────────────────────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidIntegrationEvent event) {
        safely("order-confirmation-mail", event.orderId(), () -> orderHandler.onOrderPaid(event));
    }

    // ── Subscription ────────────────────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionCancelled(SubscriptionCancelledIntegrationEvent event) {
        safely("subscription-cancelled-mail", event.subscriptionId(), () -> subscriptionHandler.onCancelled(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionPaymentFailed(SubscriptionPaymentFailedIntegrationEvent event) {
        safely("subscription-pastdue-mail", event.subscriptionId(), () -> subscriptionHandler.onPaymentFailed(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionPaymentActionRequired(SubscriptionPaymentActionRequiredIntegrationEvent event) {
        safely("subscription-action-required-mail", event.subscriptionId(),
                () -> subscriptionHandler.onPaymentActionRequired(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionAutoRenewReminderDue(SubscriptionAutoRenewReminderDueIntegrationEvent event) {
        safely("subscription-renewal-reminder", event.subscriptionId(), () -> subscriptionHandler.onAutoRenewReminderDue(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionTrialWillEnd(SubscriptionTrialWillEndIntegrationEvent event) {
        safely("subscription-trial-will-end-mail", event.subscriptionId(),
                () -> subscriptionHandler.onTrialWillEnd(event));
    }

    /**
     * Notifications are best-effort side effects: a delivery failure is logged,
     * never propagated — the producer transaction has already committed and must
     * not be affected by a downstream mail problem.
     */
    private void safely(String tag, Object correlationId, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("[{}] dispatch failed id={}", tag, correlationId, e);
        }
    }
}
