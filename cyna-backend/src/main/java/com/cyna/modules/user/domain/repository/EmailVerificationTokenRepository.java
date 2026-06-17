package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.EmailVerificationToken;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository {

    void save(EmailVerificationToken token);

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    /**
     * Removes every pending (unconsumed) token owned by the user — called
     * before issuing a new one so the user's mailbox never accumulates more
     * than one valid verification link at a time.
     */
    void deleteUnconsumedByUserId(UUID userId);
}
