package com.cyna.modules.user.infrastructure.persistence.mapper;

import com.cyna.modules.user.domain.model.TrustedDevice;
import com.cyna.modules.user.infrastructure.persistence.entity.TrustedDeviceJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TrustedDeviceJpaMapper {

    public TrustedDeviceJpaEntity toJpa(TrustedDevice device) {
        var entity = new TrustedDeviceJpaEntity();
        entity.setId(device.id());
        entity.setUserId(device.userId());
        entity.setTokenHash(device.tokenHash());
        entity.setExpiresAt(device.expiresAt());
        entity.setCreatedAt(device.createdAt());
        entity.setLastUsedAt(device.lastUsedAt());
        entity.setUserAgent(device.userAgent());
        return entity;
    }

    public TrustedDevice toDomain(TrustedDeviceJpaEntity entity) {
        return TrustedDevice.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getLastUsedAt(),
                entity.getUserAgent()
        );
    }
}
