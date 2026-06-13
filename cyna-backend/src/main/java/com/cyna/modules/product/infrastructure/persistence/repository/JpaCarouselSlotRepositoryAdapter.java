package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.CarouselSlot;
import com.cyna.modules.product.domain.repository.CarouselSlotRepository;
import com.cyna.modules.product.infrastructure.persistence.entity.CarouselSlotJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaCarouselSlotRepositoryAdapter implements CarouselSlotRepository {

    private final SpringDataCarouselSlotRepository springRepo;

    public JpaCarouselSlotRepositoryAdapter(SpringDataCarouselSlotRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void add(UUID promotionId) {
        int nextOrder = springRepo.findMaxSlotOrder() + 1;
        CarouselSlotJpaEntity entity = new CarouselSlotJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setPromotionId(promotionId);
        entity.setSlotOrder(nextOrder);
        springRepo.save(entity);
    }

    @Override
    public void remove(UUID promotionId) {
        springRepo.deleteByPromotionId(promotionId);
    }

    @Override
    public List<CarouselSlot> findAll() {
        return springRepo.findAllByOrderBySlotOrderAsc().stream()
                .map(e -> new CarouselSlot(e.getPromotionId(), e.getSlotOrder()))
                .toList();
    }

    @Override
    public int count() {
        return (int) springRepo.count();
    }

    @Override
    public boolean existsByPromotionId(UUID promotionId) {
        return springRepo.existsByPromotionId(promotionId);
    }

    @Override
    public void reorder(List<UUID> orderedPromotionIds) {
        // Delete existing slots for these promotions, flush to release unique constraint,
        // then re-insert with consecutive orders 1..N.
        springRepo.deleteAllByPromotionIdIn(orderedPromotionIds);
        springRepo.flush();

        for (int i = 0; i < orderedPromotionIds.size(); i++) {
            CarouselSlotJpaEntity entity = new CarouselSlotJpaEntity();
            entity.setId(UUID.randomUUID());
            entity.setPromotionId(orderedPromotionIds.get(i));
            entity.setSlotOrder(i + 1);
            springRepo.save(entity);
        }
    }
}
