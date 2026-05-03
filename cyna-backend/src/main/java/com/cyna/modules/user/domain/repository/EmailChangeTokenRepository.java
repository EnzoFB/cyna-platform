package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.EmailChangeToken;

import java.util.Optional;
import java.util.UUID;

public interface EmailChangeTokenRepository {
    void save(EmailChangeToken token);
    Optional<EmailChangeToken> findByToken(String token);
    void deleteByUserId(UUID userId);
    void deleteById(UUID id);
}
