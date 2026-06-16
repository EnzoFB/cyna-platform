package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.domain.model.ConsentAction;
import com.cyna.modules.payment.domain.model.PaymentConsentLog;
import com.cyna.modules.payment.domain.repository.PaymentConsentLogRepository;
import com.cyna.modules.payment.infrastructure.persistence.entity.PaymentConsentLogJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class JpaPaymentConsentLogRepositoryAdapter implements PaymentConsentLogRepository {

    private final SpringDataPaymentConsentLogRepository springRepo;

    JpaPaymentConsentLogRepositoryAdapter(SpringDataPaymentConsentLogRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public PaymentConsentLog save(PaymentConsentLog log) {
        PaymentConsentLogJpaEntity entity = new PaymentConsentLogJpaEntity();
        entity.setId(log.getId());
        entity.setUserId(log.getUserId());
        entity.setAction(log.getAction().name());
        entity.setLabelVersion(log.getLabelVersion());
        entity.setStripePaymentMethodId(log.getStripePaymentMethodId());
        entity.setIpAddress(log.getIpAddress());
        entity.setUserAgent(log.getUserAgent());
        entity.setGivenAt(log.getGivenAt());
        springRepo.save(entity);
        return log;
    }

    @Override
    public List<PaymentConsentLog> findAllByUserId(UUID userId) {
        return springRepo.findByUserIdOrderByGivenAtDesc(userId).stream()
                .map(e -> PaymentConsentLog.reconstitute(
                        e.getId(), e.getUserId(), ConsentAction.valueOf(e.getAction()),
                        e.getLabelVersion(), e.getStripePaymentMethodId(),
                        e.getIpAddress(), e.getUserAgent(), e.getGivenAt()))
                .toList();
    }
}
