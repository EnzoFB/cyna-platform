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
}
