package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.domain.model.OrderTaxSnapshot;
import com.cyna.modules.payment.domain.repository.OrderTaxSnapshotRepository;
import com.cyna.modules.payment.infrastructure.persistence.entity.OrderTaxSnapshotJpaEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JpaOrderTaxSnapshotRepositoryAdapter implements OrderTaxSnapshotRepository {

    private final SpringDataOrderTaxSnapshotRepository springRepo;

    JpaOrderTaxSnapshotRepositoryAdapter(SpringDataOrderTaxSnapshotRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<OrderTaxSnapshot> findByOrderIdAndUserId(UUID orderId, UUID userId) {
        return springRepo.findByOrderIdAndUserId(orderId, userId).map(this::toDomain);
    }

    @Override
    public void save(OrderTaxSnapshot snapshot) {
        if (springRepo.existsById(snapshot.orderId())) {
            // First capture wins — the checkout invoice is immutable, so a retry
            // of finalize must not overwrite it.
            return;
        }
        var entity = new OrderTaxSnapshotJpaEntity();
        entity.setOrderId(snapshot.orderId());
        entity.setUserId(snapshot.userId());
        entity.setSubtotalHt(snapshot.subtotalHt());
        entity.setVatAmount(snapshot.vatAmount());
        entity.setTotalTtc(snapshot.totalTtc());
        entity.setCurrency(snapshot.currency());
        entity.setReverseCharge(snapshot.reverseCharge());
        entity.setCapturedAt(snapshot.capturedAt());
        try {
            springRepo.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Race: another thread captured concurrently — safe to ignore.
        }
    }

    private OrderTaxSnapshot toDomain(OrderTaxSnapshotJpaEntity e) {
        return new OrderTaxSnapshot(
                e.getOrderId(),
                e.getUserId(),
                e.getSubtotalHt(),
                e.getVatAmount(),
                e.getTotalTtc(),
                e.getCurrency(),
                e.isReverseCharge(),
                e.getCapturedAt());
    }
}
