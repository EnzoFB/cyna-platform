package com.cyna.modules.user.infrastructure.security;

import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.HashedPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public HashedPassword hash(String rawPassword) {
        return HashedPassword.of(encoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, HashedPassword hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword.value());
    }
}
