package com.cyna.modules.notification.infrastructure.event;

import com.cyna.modules.notification.application.eventhandler.OrderNotificationHandler;
import com.cyna.modules.notification.application.eventhandler.SubscriptionNotificationHandler;
import com.cyna.modules.notification.application.eventhandler.UserNotificationHandler;
import com.cyna.modules.order.domain.event.OrderPaid;
import com.cyna.modules.subscription.domain.event.SubscriptionAutoRenewReminderDue;
import com.cyna.modules.subscription.domain.event.SubscriptionCancelled;
import com.cyna.modules.subscription.domain.event.SubscriptionPaymentFailed;
import com.cyna.modules.user.domain.event.EmailChangeRequested;
import com.cyna.modules.user.domain.event.LoginOtpRequested;
import com.cyna.modules.user.domain.event.PasswordResetRequested;
import com.cyna.modules.user.domain.event.SuspiciousAuthActivityDetected;
import com.cyna.modules.user.domain.event.UserEmailChanged;
import com.cyna.modules.user.domain.event.UserPasswordChanged;
import com.cyna.modules.user.domain.event.UserRegistered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The notification module's single subscription point to the rest of the
 * platform. Every method reacts to a <b>published domain event</b> of another
 * module ({@code order.domain.event}, {@code subscription.domain.event},
 * {@code user.domain.event}) — the only inbound coupling allowed by the
 * module-isolation rule.
 *
 * <p>Listeners live in {@code infrastructure/event} (not {@code interfaces})
 * because the ArchUnit rule {@code interfacesMustNotDependOnModuleDomain}
 * forbids the interfaces layer from importing another module's {@code domain..}
 * types — and the event types reside in {@code domain.event}. Each listener is
 * a thin, {@code AFTER_COMMIT} adapter: it delegates straight to an application
 * handler and never lets a delivery failure bubble back into the committed
 * producer transaction.
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
    public void onUserRegistered(UserRegistered event) {
        safely("welcome-email", event.userId(), () -> userHandler.onUserRegistered(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(UserPasswordChanged event) {
        safely("password-changed-alert", event.userId(), () -> userHandler.onPasswordChanged(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailChanged(UserEmailChanged event) {
        safely("email-changed-alert", event.userId(), () -> userHandler.onEmailChanged(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequested event) {
        safely("password-reset-request", event.userId(), () -> userHandler.onPasswordResetRequested(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSuspiciousActivity(SuspiciousAuthActivityDetected event) {
        safely("suspicious-activity-alert", event.userId(), () -> userHandler.onSuspiciousActivity(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoginOtpRequested(LoginOtpRequested event) {
        safely("login-otp-mail", event.email(), () -> userHandler.onLoginOtpRequested(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailChangeRequested(EmailChangeRequested event) {
        safely("email-change-confirmation", event.userId(), () -> userHandler.onEmailChangeRequested(event));
    }

    // ── Order ─────────────────────────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaid event) {
        safely("order-confirmation-mail", event.orderId(), () -> orderHandler.onOrderPaid(event));
    }

    // ── Subscription ────────────────────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionCancelled(SubscriptionCancelled event) {
        safely("subscription-cancelled-mail", event.subscriptionId(), () -> subscriptionHandler.onCancelled(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionPaymentFailed(SubscriptionPaymentFailed event) {
        safely("subscription-pastdue-mail", event.subscriptionId(), () -> subscriptionHandler.onPaymentFailed(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubscriptionAutoRenewReminderDue(SubscriptionAutoRenewReminderDue event) {
        safely("subscription-renewal-reminder", event.subscriptionId(), () -> subscriptionHandler.onAutoRenewReminderDue(event));
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
