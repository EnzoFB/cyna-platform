package com.cyna.modules.user.infrastructure.persistence.mapper;

import com.cyna.modules.user.domain.model.LoginOtpChallenge;
import com.cyna.modules.user.infrastructure.persistence.entity.LoginOtpChallengeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class LoginOtpChallengeJpaMapper {

    public LoginOtpChallengeJpaEntity toJpa(LoginOtpChallenge challenge) {
        var entity = new LoginOtpChallengeJpaEntity();
        entity.setId(challenge.id());
        entity.setUserId(challenge.userId());
        entity.setOtpHash(challenge.otpHash());
        entity.setExpiresAt(challenge.expiresAt());
        entity.setConsumed(challenge.consumed());
        entity.setCreatedAt(challenge.createdAt());
        entity.setConsumedAt(challenge.consumedAt());
        return entity;
    }

    public LoginOtpChallenge toDomain(LoginOtpChallengeJpaEntity entity) {
        return LoginOtpChallenge.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getOtpHash(),
                entity.getExpiresAt(),
                entity.isConsumed(),
                entity.getCreatedAt(),
                entity.getConsumedAt()
        );
    }
}
