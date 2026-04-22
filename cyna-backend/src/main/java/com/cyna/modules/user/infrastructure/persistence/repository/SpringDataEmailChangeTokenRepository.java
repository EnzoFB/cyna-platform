package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.infrastructure.persistence.entity.EmailChangeTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataEmailChangeTokenRepository extends JpaRepository<EmailChangeTokenJpaEntity, UUID> {
    Optional<EmailChangeTokenJpaEntity> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailChangeTokenJpaEntity e WHERE e.userId = :userId")
    void deleteByUserId(UUID userId);
}
