package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.domain.model.Payment;
import com.cyna.modules.payment.domain.repository.PaymentRepository;
import com.cyna.modules.payment.infrastructure.persistence.mapper.PaymentJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JpaPaymentRepositoryAdapter implements PaymentRepository {

    private final SpringDataPaymentRepository springRepo;
    private final PaymentJpaMapper mapper;

    JpaPaymentRepositoryAdapter(SpringDataPaymentRepository springRepo, PaymentJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Payment payment) {
        springRepo.save(mapper.toEntity(payment));
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return springRepo.findByOrderId(orderId).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId) {
        return springRepo.findByStripePaymentIntentId(stripePaymentIntentId).map(mapper::toDomain);
    }
}
