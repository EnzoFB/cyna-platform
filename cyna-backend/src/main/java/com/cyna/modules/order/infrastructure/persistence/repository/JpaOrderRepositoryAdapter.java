package com.cyna.modules.order.infrastructure.persistence.repository;

import com.cyna.modules.order.domain.repository.OrderSort;
import com.cyna.modules.order.domain.repository.OrderSortDirection;
import com.cyna.modules.order.domain.model.Order;
import com.cyna.modules.order.domain.repository.OrderRepository;
import com.cyna.modules.order.infrastructure.persistence.mapper.OrderJpaMapper;
import com.cyna.shared.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository springRepo;
    private final OrderJpaMapper mapper;

    public JpaOrderRepositoryAdapter(SpringDataOrderRepository springRepo, OrderJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Order order) {
        springRepo.save(mapper.toJpa(order));
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Order> findAllByUserId(UUID userId, int page, int size, OrderSort sort) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        org.springframework.data.domain.Page<com.cyna.modules.order.infrastructure.persistence.entity.OrderJpaEntity> result =
                springRepo.findByUserId(userId, pageable);

        List<Order> items = result.getContent().stream().map(mapper::toDomain).toList();
        return new Page<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return springRepo.existsByUserId(userId);
    }

    @Override
    public List<Order> findAllByUserId(UUID userId) {
        return springRepo.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    private Sort buildSort(OrderSort sort) {
        Sort.Direction direction = sort.direction() == OrderSortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, sort.field().jpaProperty());
    }
}
