package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.RefreshToken;
import com.cyna.modules.user.domain.model.UserId;

import java.util.Optional;

public interface RefreshTokenRepository {

    void save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllByUserId(UserId userId);
}
