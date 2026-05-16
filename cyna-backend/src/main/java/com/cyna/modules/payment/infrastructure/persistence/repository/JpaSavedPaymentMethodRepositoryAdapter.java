package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.domain.model.SavedPaymentMethod;
import com.cyna.modules.payment.domain.repository.SavedPaymentMethodRepository;
import com.cyna.modules.payment.infrastructure.persistence.entity.SavedPaymentMethodJpaEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaSavedPaymentMethodRepositoryAdapter implements SavedPaymentMethodRepository {

    private final SpringDataSavedPaymentMethodRepository springRepo;

    JpaSavedPaymentMethodRepositoryAdapter(SpringDataSavedPaymentMethodRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public List<SavedPaymentMethod> findAllByUserId(UUID userId) {
        return springRepo.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SavedPaymentMethod> findByIdAndUserId(UUID id, UUID userId) {
        return springRepo.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public Optional<SavedPaymentMethod> findByStripePaymentMethodId(String stripePaymentMethodId) {
        return springRepo.findByStripePaymentMethodId(stripePaymentMethodId).map(this::toDomain);
    }

    @Override
    public SavedPaymentMethod save(SavedPaymentMethod method) {
        springRepo.save(toEntity(method));
        return method;
    }

    @Override
    public void deleteById(UUID id) {
        springRepo.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByStripePaymentMethodId(String stripePaymentMethodId) {
        springRepo.deleteByStripePaymentMethodId(stripePaymentMethodId);
    }

    @Override
    @Transactional
    public void deleteAllByUserId(UUID userId) {
        springRepo.deleteAllByUserId(userId);
    }

    @Override
    @Transactional
    public void clearDefaultForUser(UUID userId) {
        springRepo.clearDefaultForUser(userId);
    }

    @Override
    public long countByUserId(UUID userId) {
        return springRepo.countByUserId(userId);
    }

    private SavedPaymentMethodJpaEntity toEntity(SavedPaymentMethod m) {
        var e = new SavedPaymentMethodJpaEntity();
        e.setId(m.getId());
        e.setUserId(m.getUserId());
        e.setStripePaymentMethodId(m.getStripePaymentMethodId());
        e.setBrand(m.getBrand());
        e.setLast4(m.getLast4());
        e.setExpMonth(m.getExpMonth());
        e.setExpYear(m.getExpYear());
        e.setHolderName(m.getHolderName());
        e.setDefault(m.isDefault());
        e.setCreatedAt(m.getCreatedAt());
        e.setUpdatedAt(m.getUpdatedAt());
        return e;
    }

    private SavedPaymentMethod toDomain(SavedPaymentMethodJpaEntity e) {
        return SavedPaymentMethod.reconstitute(
                e.getId(), e.getUserId(), e.getStripePaymentMethodId(),
                e.getBrand(), e.getLast4(), e.getExpMonth(), e.getExpYear(),
                e.getHolderName(), e.isDefault(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
