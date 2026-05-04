package com.cyna.modules.subscription.infrastructure.persistence.repository;

import com.cyna.modules.subscription.application.query.list.SubscriptionSort;
import com.cyna.modules.subscription.application.query.list.SubscriptionSortDirection;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.modules.subscription.infrastructure.persistence.mapper.SubscriptionJpaMapper;
import com.cyna.shared.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaSubscriptionRepositoryAdapter implements SubscriptionRepository {

    private final SpringDataSubscriptionRepository springRepo;
    private final SubscriptionJpaMapper mapper;

    public JpaSubscriptionRepositoryAdapter(SpringDataSubscriptionRepository springRepo, SubscriptionJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Subscription subscription) {
        springRepo.save(mapper.toJpa(subscription));
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Subscription> findAllByUserId(UUID userId, int page, int size, SubscriptionSort sort) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        org.springframework.data.domain.Page<com.cyna.modules.subscription.infrastructure.persistence.entity.SubscriptionJpaEntity> result =
                springRepo.findByUserId(userId, pageable);

        List<Subscription> items = result.getContent().stream().map(mapper::toDomain).toList();
        return new Page<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public List<Subscription> findAllByStripeSubscriptionId(String stripeSubscriptionId) {
        return springRepo.findAllByStripeSubscriptionId(stripeSubscriptionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    private Sort buildSort(SubscriptionSort sort) {
        Sort.Direction direction = sort.direction() == SubscriptionSortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, sort.field().jpaProperty());
    }
}
