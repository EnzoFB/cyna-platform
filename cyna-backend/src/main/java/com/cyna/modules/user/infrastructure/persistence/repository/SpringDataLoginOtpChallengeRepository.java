package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.infrastructure.persistence.entity.LoginOtpChallengeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataLoginOtpChallengeRepository extends JpaRepository<LoginOtpChallengeJpaEntity, UUID> {
}
