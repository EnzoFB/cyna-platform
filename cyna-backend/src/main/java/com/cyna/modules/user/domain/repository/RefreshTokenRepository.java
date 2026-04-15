package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    void save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);
}
