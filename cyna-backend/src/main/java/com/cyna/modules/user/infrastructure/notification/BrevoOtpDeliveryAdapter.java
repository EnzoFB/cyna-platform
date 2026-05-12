package com.cyna.modules.user.infrastructure.notification;

import com.cyna.modules.user.application.port.OtpDeliveryPort;
import com.cyna.shared.application.notification.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Primary
public class BrevoOtpDeliveryAdapter implements OtpDeliveryPort {

    private static final Logger log = LoggerFactory.getLogger(BrevoOtpDeliveryAdapter.class);

    private final MailService mailService;

    public BrevoOtpDeliveryAdapter(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public void sendLoginOtp(String email, String otpCode, Instant expiresAt, String lang) {
        log.info("Sending login OTP email to {} (expires at {})", email, expiresAt);
        mailService.sendLoginOtpEmail(email, otpCode, expiresAt, lang);
    }
}
