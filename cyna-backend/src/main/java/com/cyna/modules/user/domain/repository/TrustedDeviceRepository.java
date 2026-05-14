package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.TrustedDevice;

import java.util.Optional;
import java.util.UUID;

public interface TrustedDeviceRepository {

    void save(TrustedDevice device);

    Optional<TrustedDevice> findByTokenHash(String tokenHash);

    void deleteAllByUserId(UUID userId);
}
