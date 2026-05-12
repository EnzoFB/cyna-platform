package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {

    void save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Removes every pending (unconsumed) token owned by the user — called
     * before issuing a new one so the user's mailbox never accumulates more
     * than one valid reset link at a time.
     */
    void deleteUnconsumedByUserId(UUID userId);
}
