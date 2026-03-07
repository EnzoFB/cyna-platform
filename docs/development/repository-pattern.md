# Repository Pattern

## Purpose

This document defines the Repository pattern implementation in the CYNA Platform. The repository abstracts data access, enabling the domain layer to remain independent of any persistence technology.

---

## Overview

In Clean Architecture, the domain layer defines **repository interfaces** (ports). The infrastructure layer provides **implementations** (adapters) using JPA and Spring Data.

```
Domain Layer                          Infrastructure Layer
┌──────────────────────┐              ┌──────────────────────────────┐
│  OrderRepository     │◄──────────── │  JpaOrderRepositoryAdapter   │
│  (interface)         │  implements  │  (class)                     │
└──────────────────────┘              └──────────────────────────────┘
```

This inversion of dependencies ensures that:
- The domain has **zero coupling** to JPA, Hibernate, or Spring Data.
- The persistence technology can be swapped without changing domain code.
- Domain logic is easily testable with in-memory implementations.

---

## Domain Repository Interface

The repository interface is defined in the domain layer and operates exclusively on domain objects:

```java
package com.cyna.order.domain.repository;

import com.cyna.order.domain.model.Order;
import com.cyna.order.domain.model.OrderId;
import com.cyna.order.domain.model.CustomerId;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(OrderId id);

    List<Order> findByCustomerId(CustomerId customerId);

    boolean existsById(OrderId id);

    void deleteById(OrderId id);
}
```

### Rules for Repository Interfaces

| Rule | Rationale |
|------|-----------|
| Defined in the domain layer | Domain controls data access contracts |
| Operates on domain objects (aggregates, value objects) | No JPA entities leak into the domain |
| Uses domain value types for IDs (`OrderId`, not `UUID`) | Type safety |
| No Spring/JPA annotations | Domain purity |
| No `Page` or `Pageable` from Spring Data | Framework coupling |
| Named `{Aggregate}Repository` | Convention |
| One repository per aggregate root | DDD rule — repositories manage aggregates |

### Pagination Without Spring

If pagination is needed, define a framework-free page model:

```java
package com.cyna.shared.domain;

public record Page<T>(
    List<T> items,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
) {
    public boolean hasNext() {
        return pageNumber < totalPages - 1;
    }
}
```

Then use it in the repository interface:

```java
Page<Order> findAll(int pageNumber, int pageSize);
```

---

## Infrastructure Implementation

### JPA Repository Adapter

The adapter implements the domain repository interface and delegates to Spring Data:

```java
package com.cyna.order.infrastructure.persistence.repository;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository springRepo;
    private final OrderJpaMapper mapper;

    public JpaOrderRepositoryAdapter(SpringDataOrderRepository springRepo,
                                     OrderJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Order order) {
        OrderJpaEntity entity = mapper.toJpa(order);
        springRepo.save(entity);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return springRepo.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return springRepo.findByCustomerId(customerId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(OrderId id) {
        return springRepo.existsById(id.value());
    }

    @Override
    public void deleteById(OrderId id) {
        springRepo.deleteById(id.value());
    }
}
```

### Spring Data Repository

The Spring Data repository is an infrastructure detail, never referenced outside the infrastructure layer:

```java
package com.cyna.order.infrastructure.persistence.repository;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findByCustomerId(UUID customerId);
}
```

### JPA Entity

JPA entities are separate from domain entities. They exist only in the infrastructure layer:

```java
package com.cyna.order.infrastructure.persistence.entity;

@Entity
@Table(name = "orders", schema = "order_schema")
public class OrderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private String status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    // Getters and setters (JPA requires them)
    // ...
}
```

### JPA Mapper

The mapper translates between domain objects and JPA entities:

```java
package com.cyna.order.infrastructure.persistence.mapper;

@Component
public class OrderJpaMapper {

    public OrderJpaEntity toJpa(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getId().value());
        entity.setCustomerId(order.getCustomerId().value());
        entity.setStatus(order.getStatus().name());
        entity.setTotalAmount(order.getTotalAmount().amount());
        entity.setCurrency(order.getTotalAmount().currency());
        entity.setCreatedAt(order.getCreatedAt());

        List<OrderLineJpaEntity> lineEntities = order.getLines().stream()
                .map(line -> toJpaLine(line, entity))
                .toList();
        entity.setLines(lineEntities);

        return entity;
    }

    public Order toDomain(OrderJpaEntity entity) {
        List<OrderLine> lines = entity.getLines().stream()
                .map(this::toDomainLine)
                .toList();

        return Order.reconstitute(
                OrderId.of(entity.getId()),
                CustomerId.of(entity.getCustomerId()),
                OrderStatus.valueOf(entity.getStatus()),
                lines,
                Money.of(entity.getTotalAmount(), entity.getCurrency()),
                entity.getCreatedAt()
        );
    }

    private OrderLineJpaEntity toJpaLine(OrderLine line, OrderJpaEntity order) {
        OrderLineJpaEntity entity = new OrderLineJpaEntity();
        entity.setId(line.getId().value());
        entity.setOrder(order);
        entity.setProductId(line.getProductId().value());
        entity.setProductName(line.getProductName());
        entity.setQuantity(line.getQuantity());
        entity.setUnitPrice(line.getUnitPrice().amount());
        return entity;
    }

    private OrderLine toDomainLine(OrderLineJpaEntity entity) {
        return OrderLine.reconstitute(
                OrderLineId.of(entity.getId()),
                ProductId.of(entity.getProductId()),
                entity.getProductName(),
                entity.getQuantity(),
                Money.of(entity.getUnitPrice(), "EUR")
        );
    }
}
```

---

## Reconstitution Pattern

Domain aggregates need a way to be rebuilt from persisted data without re-validating invariants or raising creation events. Use a `reconstitute` factory method:

```java
public class Order extends AggregateRoot<OrderId> {

    // Public factory — creates NEW order (validates, raises events)
    public static Order create(CustomerId customerId, List<OrderLine> lines) {
        // Validates invariants
        // Raises OrderCreated event
        return new Order(...);
    }

    // Package-private factory — rebuilds from DB (no validation, no events)
    static Order reconstitute(OrderId id, CustomerId customerId, OrderStatus status,
                              List<OrderLine> lines, Money totalAmount, Instant createdAt) {
        Order order = new Order();
        order.id = id;
        order.customerId = customerId;
        order.status = status;
        order.lines = new ArrayList<>(lines);
        order.totalAmount = totalAmount;
        order.createdAt = createdAt;
        return order;
    }
}
```

---

## Testing with In-Memory Repositories

For unit tests, create in-memory implementations of repository interfaces:

```java
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> store = new LinkedHashMap<>();

    @Override
    public void save(Order order) {
        store.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return store.values().stream()
                .filter(o -> o.getCustomerId().equals(customerId))
                .toList();
    }

    @Override
    public boolean existsById(OrderId id) {
        return store.containsKey(id);
    }

    @Override
    public void deleteById(OrderId id) {
        store.remove(id);
    }
}
```

---

## Rules Summary

| Rule | Enforcement |
|------|-------------|
| One repository per aggregate root | Code review |
| Repository interface in domain layer | ArchUnit |
| Repository implementation in infrastructure layer | ArchUnit |
| Operates on domain objects, not JPA entities | Code review |
| JPA entities separate from domain entities | ArchUnit |
| Never expose Spring Data repository outside infrastructure | ArchUnit |
| Use `reconstitute()` for rebuilding from database | Code review |
| Provide in-memory implementations for testing | Code review |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Using `JpaRepository` directly in handlers | Create a domain repository interface |
| Returning `JpaEntity` from repository | Map to domain objects |
| Putting business logic in the adapter | Logic belongs in domain and application layers |
| Injecting `EntityManager` in application layer | Only use in infrastructure |
| Using `@Transactional` on repository methods | Use TransactionRunner in command handlers |
| Repository for non-aggregate entities | Only aggregate roots get repositories |

---

## Related Documents

- [Patterns](patterns.md)
- [Transaction Management](transaction-management.md)
- [Aggregates](../domain/aggregates.md)
- [Dependency Rules](../architecture/dependency-rules.md)
