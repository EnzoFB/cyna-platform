package com.cyna.shared.infrastructure.notification;

public interface MailService {
    void sendWelcomeEmail(String email, String firstName, String lang);
    void sendOrderConfirmation(String email, String orderId, String lang);
    void sendEmailChangeConfirmation(String email, String firstName, String token, String lang);
    void sendLoginOtpEmail(String email, String otpCode, java.time.Instant expiresAt, String lang);
}
