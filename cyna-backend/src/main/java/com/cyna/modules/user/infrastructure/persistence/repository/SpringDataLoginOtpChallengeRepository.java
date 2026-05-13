package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.infrastructure.persistence.entity.LoginOtpChallengeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface SpringDataLoginOtpChallengeRepository extends JpaRepository<LoginOtpChallengeJpaEntity, UUID> {

    @Modifying
    @Query("delete from LoginOtpChallengeJpaEntity c where c.userId = :userId and c.consumed = false")
    void deleteUnconsumedByUserId(UUID userId);
}
