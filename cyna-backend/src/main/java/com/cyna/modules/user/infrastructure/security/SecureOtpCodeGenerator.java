package com.cyna.modules.user.infrastructure.security;

import com.cyna.modules.user.application.port.OtpCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureOtpCodeGenerator implements OtpCodeGenerator {

    private static final int NUMERIC_BOUND = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateNumericCode(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("OTP length must be greater than 0");
        }

        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(secureRandom.nextInt(NUMERIC_BOUND));
        }
        return code.toString();
    }
}
