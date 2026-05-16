package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.modules.payment.infrastructure.persistence.entity.StripeCustomerJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaStripeCustomerRepositoryAdapter implements StripeCustomerRepository {

    private final SpringDataStripeCustomerRepository springRepo;

    JpaStripeCustomerRepositoryAdapter(SpringDataStripeCustomerRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<String> findStripeCustomerIdByUserId(UUID userId) {
        return springRepo.findById(userId).map(StripeCustomerJpaEntity::getStripeCustomerId);
    }

    @Override
    public Optional<UUID> findUserIdByStripeCustomerId(String stripeCustomerId) {
        return springRepo.findByStripeCustomerId(stripeCustomerId)
                .map(StripeCustomerJpaEntity::getUserId);
    }

    @Override
    public void save(UUID userId, String stripeCustomerId) {
        var entity = new StripeCustomerJpaEntity();
        entity.setUserId(userId);
        entity.setStripeCustomerId(stripeCustomerId);
        entity.setCreatedAt(Instant.now());
        springRepo.save(entity);
    }
}
