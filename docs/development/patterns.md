# Patterns

## Purpose

This document provides an overview of all design patterns used in the CYNA Platform. Each pattern is introduced with its purpose, how it fits into the architecture, and a link to its detailed documentation.

---

## Pattern Map

```
┌───────────────────────────────────────────────────────────────┐
│                    CYNA Platform Patterns                     │
│                                                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐    │
│  │  Mediator   │  │  CQRS       │  │  Result Pattern     │    │
│  │  Pattern    │  │             │  │                     │    │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘    │
│         │                │                     │              │
│         └────────────────┼─────────────────────┘              │
│                          │                                    │
│  ┌─────────────┐  ┌─────▼───────┐  ┌─────────────────────┐    │
│  │  Repository │  │  Domain     │  │  Unit of Work /     │    │
│  │  Pattern    │  │  Events     │  │  TransactionRunner  │    │
│  └─────────────┘  └─────────────┘  └─────────────────────┘    │
└───────────────────────────────────────────────────────────────┘
```

---

## 1. Mediator Pattern

**What**: A central dispatcher routes commands and queries to their respective handlers. Controllers never directly instantiate or call handlers.

**Why**: Decouples the HTTP layer from the application layer. Controllers depend only on the Mediator interface, not on individual handlers.

**Where used**: Interfaces layer (controllers) → Application layer (handlers).

```java
// Controller sends command through Mediator
Result<OrderId> result = mediator.send(new CreateOrderCommand(...));
```

**Detailed documentation**: [Mediator Pattern](mediator-pattern.md)

---

## 2. CQRS (Command Query Responsibility Segregation)

**What**: Commands (writes) and queries (reads) are handled by separate objects. Each has its own handler, its own input type, and its own return type.

**Why**: Separation of concerns. Write models enforce invariants; read models are optimized for consumption. Each handler has a single responsibility.

**Where used**: Application layer.

| Type | Purpose | Returns |
|------|---------|---------|
| Command | Change state | `Result<T>` (e.g., `Result<OrderId>`) |
| Query | Read data | Read model DTO (e.g., `OrderReadModel`) |

```java
// Command — changes state
public record CreateOrderCommand(CustomerId customerId, List<OrderItemDto> items) implements Command<OrderId> {}

// Query — reads data
public record GetOrderByIdQuery(OrderId orderId) implements Query<OrderReadModel> {}
```

**Rules**:
- A command handler must never return domain entities.
- A query handler must never modify state.
- One handler per command/query.
- Commands go through the write model (domain aggregates).
- Queries can go directly to the database (read model) for performance.

---

## 3. Result Pattern

**What**: Operations return a `Result<T>` object instead of throwing exceptions for business failures.

**Why**: Makes error paths explicit and composable. The caller must handle both success and failure cases. Avoids using exceptions for flow control.

**Where used**: Domain layer and application layer.

```java
Result<Void> result = order.confirm();
if (result.isFailure()) {
    return Result.failure(result.getError());
}
```

**Detailed documentation**: [Result Pattern](result-pattern.md)

---

## 4. Repository Pattern

**What**: Domain-layer interfaces define contracts for aggregate persistence. Infrastructure-layer adapters implement these interfaces using JPA.

**Why**: Decouples the domain from the database technology. Domain objects are unaware of how they are stored.

**Where used**: Domain layer (interface) → Infrastructure layer (implementation).

```java
// Domain — interface
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
}

// Infrastructure — implementation
@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository { ... }
```

**Detailed documentation**: [Repository Pattern](repository-pattern.md)

---

## 5. Unit of Work / TransactionRunner

**What**: A `TransactionRunner` abstraction allows the application layer to run code within a transaction boundary without depending on Spring's `@Transactional`.

**Why**: Keeps the application layer framework-free. Transaction management is an infrastructure concern.

**Where used**: Application layer (interface) → Infrastructure layer (implementation).

```java
transactionRunner.run(() -> {
    Order order = Order.create(customerId, lines);
    orderRepository.save(order);
    eventPublisher.publishAll(order.getDomainEvents());
});
```

**Detailed documentation**: [Transaction Management](transaction-management.md)

---

## 6. Domain Events

**What**: Aggregates raise events when significant state changes occur. Other modules subscribe to these events to react asynchronously.

**Why**: Loose coupling between modules. The publisher does not know about — or depend on — subscribers.

**Where used**: Domain layer (raise) → Infrastructure layer (publish) → Other modules (subscribe).

```java
// In aggregate
raise(new OrderConfirmed(getId().value(), Instant.now()));

// In subscriber
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(OrderConfirmed event) { ... }
```

**Detailed documentation**: [Domain Events](../domain/domain-events.md)

---

## 7. Anti-Corruption Layer (ACL)

**What**: A translation layer between the domain and external systems (payment providers, third-party APIs). It converts external models into domain-compatible representations.

**Why**: Protects the domain from external API changes and inconsistent data formats.

**Where used**: Infrastructure layer.

```java
// ACL mapper in infrastructure
public class StripePaymentAclMapper {
    public static Payment toDomain(StripePaymentIntent stripeIntent) {
        return Payment.reconstitute(
            PaymentId.of(UUID.fromString(stripeIntent.getMetadata().get("paymentId"))),
            Money.of(stripeIntent.getAmount(), stripeIntent.getCurrency()),
            mapStatus(stripeIntent.getStatus())
        );
    }
}
```

---

## 8. Specification Pattern (Optional / Future)

**What**: Encapsulates query criteria into composable, reusable objects.

**Why**: Enables complex filtering logic without polluting repository interfaces with many specialized query methods.

```java
Specification<Product> spec = ProductSpecifications.isPublished()
    .and(ProductSpecifications.inCategory(categoryId))
    .and(ProductSpecifications.priceBetween(minPrice, maxPrice));

List<Product> products = productRepository.findAll(spec);
```

---

## Pattern Decision Matrix

| Scenario | Pattern | Rationale |
|----------|---------|-----------|
| HTTP request needs to execute a use case | Mediator | Decouples controller from handler |
| Business operation changes state | Command + Handler | CQRS write path |
| Client needs to read data | Query + Handler | CQRS read path |
| Business operation can fail | Result Pattern | Explicit error handling |
| Need to persist/retrieve an aggregate | Repository Pattern | Data access abstraction |
| Need transactional consistency | TransactionRunner | Framework-free transactions |
| Module needs to react to another module's changes | Domain Events | Loose coupling |
| Integrating with external API | Anti-Corruption Layer | Domain protection |
| Complex search/filtering | Specification Pattern | Composable queries |

---

## Related Documents

- [Mediator Pattern](mediator-pattern.md)
- [Repository Pattern](repository-pattern.md)
- [Result Pattern](result-pattern.md)
- [Transaction Management](transaction-management.md)
- [Domain Events](../domain/domain-events.md)
