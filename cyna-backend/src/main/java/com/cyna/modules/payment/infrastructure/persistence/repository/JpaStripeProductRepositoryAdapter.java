package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.domain.repository.StripeProductRepository;
import com.cyna.modules.payment.infrastructure.persistence.entity.StripeProductJpaEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaStripeProductRepositoryAdapter implements StripeProductRepository {

    private final SpringDataStripeProductRepository springRepo;

    JpaStripeProductRepositoryAdapter(SpringDataStripeProductRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<String> findStripeProductIdByCynaProductId(UUID cynaProductId) {
        return springRepo.findById(cynaProductId).map(StripeProductJpaEntity::getStripeProductId);
    }

    @Override
    public void save(UUID cynaProductId, String stripeProductId) {
        if (springRepo.existsById(cynaProductId)) {
            // Already mapped — no-op (idempotent on concurrent calls).
            return;
        }
        var entity = new StripeProductJpaEntity();
        entity.setCynaProductId(cynaProductId);
        entity.setStripeProductId(stripeProductId);
        entity.setCreatedAt(Instant.now());
        try {
            springRepo.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Race: another thread inserted concurrently — safe to ignore.
        }
    }
}
