package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.infrastructure.persistence.entity.AddressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SpringDataAddressRepository extends JpaRepository<AddressJpaEntity, UUID> {

    @Query("SELECT a FROM AddressJpaEntity a WHERE a.userId = :userId ORDER BY a.isDefault DESC, a.updatedAt DESC")
    List<AddressJpaEntity> findAllByUserIdOrdered(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE AddressJpaEntity a SET a.isDefault = false WHERE a.userId = :userId")
    void clearDefaultForUser(UUID userId);
}
