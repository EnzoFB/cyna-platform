package com.cyna.shared.infrastructure.notification;

public interface MailService {
    void sendWelcomeEmail(String email, String firstName, String lang);
    void sendOrderConfirmation(String email, String orderId, String lang);
}
