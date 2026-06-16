package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.infrastructure.persistence.entity.UserConsentLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataUserConsentLogRepository
        extends JpaRepository<UserConsentLogJpaEntity, UUID> {

    List<UserConsentLogJpaEntity> findByUserIdOrderByGivenAtDesc(UUID userId);
}
