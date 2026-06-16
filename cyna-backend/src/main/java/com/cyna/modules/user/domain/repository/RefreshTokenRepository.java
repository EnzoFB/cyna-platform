package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    void save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Same lookup as {@link #findByTokenHash(String)} but acquires a row-level
     * write lock — used by the refresh-rotation flow to serialise concurrent
     * uses of the same token (defeats the read-check-then-update race).
     * Must be called inside a transaction.
     */
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    void revokeAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);
}
