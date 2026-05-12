package com.cyna.shared.application.notification;

import java.time.Instant;

/**
 * Outbound port for transactional emails. Lives in the application layer so
 * module command handlers can depend on it without crossing into infrastructure
 * — the concrete adapter ({@code BrevoMailService}) lives in
 * {@code com.cyna.shared.infrastructure.notification} and is wired by Spring.
 */
public interface MailService {
    void sendWelcomeEmail(String email, String firstName, String lang);
    void sendOrderConfirmation(String email, String orderId, String lang);
    void sendEmailChangeConfirmation(String email, String firstName, String token, String lang);
    void sendLoginOtpEmail(String email, String otpCode, Instant expiresAt, String lang);
    void sendSubscriptionAutoRenewReminder(String email, String firstName, String productName, String renewalDate, String lang);

    /**
     * Security alert sent to the user's registered email when their password
     * was just changed. The legitimate user must notice this if it was not
     * them — primary takeover detection signal.
     */
    void sendPasswordChangedAlert(String email, String firstName, String lang);

    /**
     * Security alert sent to the PREVIOUS email when the account email was
     * just changed. Notifies the original owner if their account was hijacked.
     */
    void sendEmailChangedAlert(String previousEmail, String newEmail, String firstName, String lang);

    /**
     * Security alert sent when suspicious authentication activity was detected
     * (typically: refresh-token replay). All sessions have already been revoked.
     */
    void sendSuspiciousActivityAlert(String email, String firstName, String lang);

    /**
     * Delivers the one-shot reset link to the user who triggered a
     * forgot-password request. The adapter constructs the URL from the raw
     * token and the configured app base URL; the raw token only ever appears
     * in the resulting email body.
     */
    void sendPasswordResetEmail(String email, String firstName, String rawToken, String lang);
}
