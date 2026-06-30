package com.cyna.modules.user.infrastructure.persistence.mapper;

import com.cyna.modules.user.domain.model.EmailVerificationToken;
import com.cyna.modules.user.infrastructure.persistence.entity.EmailVerificationTokenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationTokenJpaMapper {

    public EmailVerificationTokenJpaEntity toJpa(EmailVerificationToken token) {
        var entity = new EmailVerificationTokenJpaEntity();
        entity.setId(token.id());
        entity.setUserId(token.userId());
        entity.setTokenHash(token.tokenHash());
        entity.setExpiresAt(token.expiresAt());
        entity.setConsumed(token.consumed());
        entity.setCreatedAt(token.createdAt());
        return entity;
    }

    public EmailVerificationToken toDomain(EmailVerificationTokenJpaEntity entity) {
        return new EmailVerificationToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.isConsumed(),
                entity.getCreatedAt()
        );
    }
}
