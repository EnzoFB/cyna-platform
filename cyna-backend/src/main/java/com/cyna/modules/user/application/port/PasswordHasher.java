package com.cyna.modules.user.application.port;

import com.cyna.modules.user.domain.model.HashedPassword;

public interface PasswordHasher {

    HashedPassword hash(String rawPassword);

    boolean matches(String rawPassword, HashedPassword hashedPassword);
}
