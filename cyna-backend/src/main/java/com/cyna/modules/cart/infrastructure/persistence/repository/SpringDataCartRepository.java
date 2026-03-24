package com.cyna.modules.cart.infrastructure.persistence.repository;

import com.cyna.modules.cart.infrastructure.persistence.entity.CartJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataCartRepository extends JpaRepository<CartJpaEntity, UUID> {

    Optional<CartJpaEntity> findByUserIdAndStatus(UUID userId, String status);

    Optional<CartJpaEntity> findByGuestTokenAndStatus(String guestToken, String status);

    Optional<CartJpaEntity> findTopByUserIdOrderByUpdatedAtDesc(UUID userId);
}
