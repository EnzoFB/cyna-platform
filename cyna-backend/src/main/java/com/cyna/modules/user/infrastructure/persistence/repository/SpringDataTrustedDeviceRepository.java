package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.infrastructure.persistence.entity.TrustedDeviceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataTrustedDeviceRepository extends JpaRepository<TrustedDeviceJpaEntity, UUID> {

    Optional<TrustedDeviceJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from TrustedDeviceJpaEntity d where d.userId = :userId")
    void deleteAllByUserId(UUID userId);
}
