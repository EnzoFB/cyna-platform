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
    void sendOrderConfirmation(OrderConfirmationMail data);
    void sendEmailChangeConfirmation(String email, String firstName, String token, String lang);
    void sendLoginOtpEmail(String email, String otpCode, Instant expiresAt, String lang);
    void sendSubscriptionAutoRenewReminder(String email, String firstName, String productName, String renewalDate, String lang);

    /**
     * Alert sent when a renewal payment failed and the subscription went
     * PAST_DUE. Prompts the customer to update their card before Stripe's
     * dunning gives up and cancels the subscription.
     */
    void sendSubscriptionPaymentFailed(String email, String firstName, String productName, String lang);

    /**
     * Confirmation sent when the customer cancels a subscription. Reassures
     * them it was registered and that access continues until the paid period
     * ends.
     */
    void sendSubscriptionCancellationConfirmation(String email, String firstName, String productName, String lang);

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

    /**
     * Sends a contact form submission to the configured internal contact email.
     * The email is sent from the platform sender to the configured contact address,
     * with the user's email included in the body so the team can reply directly.
     */
    void sendContactEmail(String fromEmail, String name, String subject, String message, String lang);

    /**
     * Sends an acknowledgement email to the user who submitted the contact form.
     * Confirms that the message was received and will be processed by the team.
     */
    void sendContactAcknowledgementEmail(String email, String name, String subject, String lang);
}
