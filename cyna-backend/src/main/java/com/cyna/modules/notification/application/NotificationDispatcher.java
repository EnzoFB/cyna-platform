package com.cyna.modules.notification.application;

import com.cyna.modules.notification.application.mail.OrderConfirmationMail;

import java.time.Instant;

/**
 * The notification module's internal application contract: every transactional
 * message the platform can emit, expressed in business terms (not "send an
 * email with this HTML"). Implemented by the infrastructure adapter
 * ({@code BrevoNotificationDispatcher}), which owns template rendering and
 * delivery.
 *
 * <p>This is the single place that knows <em>what</em> messages exist. Event
 * handlers (async reactions to other modules' domain events) all funnel through
 * it. No other module depends on this type — it is the module's own seam.
 */
public interface NotificationDispatcher {

    // ── Account / security lifecycle (reacted to from user domain events) ─────
    void sendWelcomeEmail(String email, String firstName, String lang);

    void sendPasswordChangedAlert(String email, String firstName, String lang);

    void sendEmailChangedAlert(String previousEmail, String newEmail, String firstName, String lang);

    void sendSuspiciousActivityAlert(String email, String firstName, String lang);

    void sendPasswordResetEmail(String email, String firstName, String rawToken, String lang);

    void sendEmailVerification(String email, String firstName, String rawToken, String lang);

    // ── Order / subscription lifecycle (reacted to from domain events) ────────
    void sendOrderConfirmation(OrderConfirmationMail data);

    void sendSubscriptionCancellationConfirmation(String email, String firstName, String productName, String lang);

    void sendSubscriptionPaymentFailed(String email, String firstName, String productName, String lang);

    /**
     * Sent on {@code invoice.payment_action_required} (PSD2 SCA on renewal).
     * {@code hostedInvoiceUrl} is the Stripe-hosted page where the customer
     * completes the 3DS challenge — embedded as the email's primary CTA.
     */
    void sendSubscriptionPaymentActionRequired(String email, String firstName, String productName,
                                               String hostedInvoiceUrl, String lang);

    // ── Sends triggered by request-time events (OTP, email-change, reminder) ──
    void sendEmailChangeConfirmation(String email, String firstName, String token, String lang);

    void sendLoginOtpEmail(String email, String otpCode, Instant expiresAt, String lang);

    void sendSubscriptionAutoRenewReminder(String email, String firstName, String productName, String renewalDate, String lang);

    /**
     * Sent on {@code customer.subscription.trial_will_end} (~3 days before the
     * free trial converts to paid). Warns the customer before the first charge —
     * which for an annual plan is the full year. {@code trialEndAt} is when the
     * trial ends and billing starts.
     */
    void sendSubscriptionTrialWillEnd(String email, String firstName, String productName, Instant trialEndAt, String lang);

    // ── Contact form (owned by this module's own REST endpoint) ───────────────
    void sendContactEmail(String fromEmail, String name, String subject, String message, String lang);

    void sendContactAcknowledgementEmail(String email, String name, String subject, String lang);
}
