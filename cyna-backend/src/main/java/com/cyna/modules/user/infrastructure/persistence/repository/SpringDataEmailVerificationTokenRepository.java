package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.infrastructure.persistence.entity.EmailVerificationTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataEmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationTokenJpaEntity, UUID> {

    Optional<EmailVerificationTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerificationTokenJpaEntity t WHERE t.userId = :userId AND t.consumed = false")
    void deleteUnconsumedByUserId(@Param("userId") UUID userId);
}
