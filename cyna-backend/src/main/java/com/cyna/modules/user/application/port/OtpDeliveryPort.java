package com.cyna.modules.user.application.port;

import java.time.Instant;

public interface OtpDeliveryPort {

    void sendLoginOtp(String email, String otpCode, Instant expiresAt, String lang);
}
