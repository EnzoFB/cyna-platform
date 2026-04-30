package com.cyna.modules.user.infrastructure.notification;

import com.cyna.modules.user.application.port.OtpDeliveryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class LoggingOtpDeliveryAdapter implements OtpDeliveryPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpDeliveryAdapter.class);

    @Override
    public void sendLoginOtp(String email, String otpCode, Instant expiresAt) {
        log.info("Login OTP generated for {}. Expires at {}", email, expiresAt);
        log.debug("Login OTP for {} is {}", email, otpCode);
    }
}
