package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.infrastructure.persistence.entity.SavedPaymentMethodJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataSavedPaymentMethodRepository
        extends JpaRepository<SavedPaymentMethodJpaEntity, UUID> {

    List<SavedPaymentMethodJpaEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<SavedPaymentMethodJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<SavedPaymentMethodJpaEntity> findByStripePaymentMethodId(String stripePaymentMethodId);

    @Modifying
    @Query("DELETE FROM SavedPaymentMethodJpaEntity e WHERE e.stripePaymentMethodId = :pmId")
    void deleteByStripePaymentMethodId(@Param("pmId") String stripePaymentMethodId);

    @Modifying
    @Query("DELETE FROM SavedPaymentMethodJpaEntity e WHERE e.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    long countByUserId(UUID userId);

    @Modifying
    @Query("UPDATE SavedPaymentMethodJpaEntity e SET e.isDefault = FALSE WHERE e.userId = :userId")
    void clearDefaultForUser(@Param("userId") UUID userId);
}
