package com.cyna.modules.user.infrastructure.persistence.mapper;

import com.cyna.modules.user.domain.model.PasswordResetToken;
import com.cyna.modules.user.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenJpaMapper {

    public PasswordResetTokenJpaEntity toJpa(PasswordResetToken token) {
        var entity = new PasswordResetTokenJpaEntity();
        entity.setId(token.id());
        entity.setUserId(token.userId());
        entity.setTokenHash(token.tokenHash());
        entity.setExpiresAt(token.expiresAt());
        entity.setConsumed(token.consumed());
        entity.setCreatedAt(token.createdAt());
        return entity;
    }

    public PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        return new PasswordResetToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.isConsumed(),
                entity.getCreatedAt()
        );
    }
}
