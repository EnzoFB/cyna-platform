# Transaction Management

## Purpose

This document describes the transaction management strategy in the CYNA Platform. Transactions are managed through a `TransactionRunner` abstraction that keeps the application layer free of Spring's `@Transactional` annotation.

---

## Design Rationale

### Why Not `@Transactional` on Handlers?

In a typical Spring Boot application, you might place `@Transactional` directly on service methods or handlers:

```java
// ❌ Couples the application layer to Spring
@Transactional
public Result<OrderId> handle(CreateOrderCommand command) { ... }
```

This violates Clean Architecture because:

1. **Framework coupling** — The application layer depends on `org.springframework.transaction.annotation`.
2. **Invisible behavior** — The transaction boundary is defined by an annotation, making it easy to overlook.
3. **Testing complexity** — Unit tests need Spring's transaction infrastructure or workarounds.

### TransactionRunner Approach

Instead, we define a `TransactionRunner` interface in the application layer and implement it in the infrastructure layer:

```
Application Layer                    Infrastructure Layer
┌──────────────────────┐             ┌──────────────────────────────┐
│  TransactionRunner   │◄────────────│  SpringTransactionRunner     │
│  (interface)         │  implements │  (@Transactional)            │
└──────────────────────┘             └──────────────────────────────┘
```

---

## TransactionRunner Interface

```java
package com.cyna.shared.application;

/**
 * Runs a unit of work within a transaction boundary.
 * The implementation manages the actual transaction lifecycle.
 */
public interface TransactionRunner {

    /**
     * Executes the given action within a transaction.
     * If the action throws an exception, the transaction is rolled back.
     *
     * @param action the code to execute within the transaction
     */
    void run(Runnable action);

    /**
     * Executes the given supplier within a transaction and returns its result.
     * If the supplier throws an exception, the transaction is rolled back.
     *
     * @param action the code to execute within the transaction
     * @param <T>    the return type
     * @return the result of the action
     */
    <T> T runReturning(Supplier<T> action);
}
```

---

## Spring Implementation

```java
package com.cyna.shared.infrastructure.transaction;

@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    @Override
    public <T> T runReturning(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}
```

---

## Usage in Command Handlers

### Basic Usage

```java
@Component
public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, OrderId> {

    private final TransactionRunner transactionRunner;
    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Result<OrderId> handle(CreateOrderCommand command) {
        return transactionRunner.runReturning(() -> {
            Order order = Order.create(command.customerId(), command.lines());
            orderRepository.save(order);
            eventPublisher.publishAll(order.getDomainEvents());
            order.clearDomainEvents();
            return Result.success(order.getId());
        });
    }
}
```

### With Error Handling

```java
@Override
public Result<Void> handle(ConfirmOrderCommand command) {
    return transactionRunner.runReturning(() -> {
        Optional<Order> orderOpt = orderRepository.findById(command.orderId());
        if (orderOpt.isEmpty()) {
            return Result.failure("Order not found: " + command.orderId());
        }

        Order order = orderOpt.get();
        Result<Void> result = order.confirm();

        if (result.isFailure()) {
            return result;
        }

        orderRepository.save(order);
        eventPublisher.publishAll(order.getDomainEvents());
        order.clearDomainEvents();
        return Result.success();
    });
}
```

### Multiple Aggregate Operations (Same Transaction)

If you must update multiple aggregates of the **same type** in one transaction (batch operation):

```java
@Override
public Result<Void> handle(CancelExpiredOrdersCommand command) {
    return transactionRunner.runReturning(() -> {
        List<Order> expiredOrders = orderRepository.findExpired(command.cutoffDate());

        for (Order order : expiredOrders) {
            Result<Void> result = order.cancel("Expired");
            if (result.isFailure()) {
                // Log and continue, or fail the whole batch
                log.warn("Could not cancel order {}: {}", order.getId(), result.getError());
                continue;
            }
            orderRepository.save(order);
            eventPublisher.publishAll(order.getDomainEvents());
            order.clearDomainEvents();
        }

        return Result.success();
    });
}
```

---

## Transaction Boundaries

### Rule: One Transaction = One Command Handler

Each command handler execution should be wrapped in a single transaction. This ensures:

- Atomicity — all changes succeed or all are rolled back.
- Consistency — invariants are maintained.
- Isolation — concurrent operations don't interfere.

### Rule: No Transactions in Queries

Query handlers should **not** use `TransactionRunner`. Reads do not require transactional guarantees (in most cases). If read consistency is needed, use a read-only transaction:

```java
@Component
public class SpringReadOnlyTransactionRunner implements ReadOnlyTransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringReadOnlyTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setReadOnly(true);
    }

    @Override
    public <T> T runReturning(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}
```

---

## Domain Event Publishing and Transactions

### Within-Transaction Events

Events that update data in the same module should be processed within the same transaction:

```java
transactionRunner.run(() -> {
    Order order = Order.create(customerId, lines);
    orderRepository.save(order);
    // Events processed synchronously, within the same transaction
    eventPublisher.publishAll(order.getDomainEvents());
    order.clearDomainEvents();
});
```

### After-Commit Events

Events that trigger side effects (email, webhooks, external API calls) should use Spring's `@TransactionalEventListener(phase = AFTER_COMMIT)` in the subscriber. The publisher does not need to worry about this — it publishes normally, and Spring defers delivery based on the listener's configuration.

```java
// Subscriber — only processes after the publisher's transaction commits
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(OrderConfirmed event) {
    emailService.sendConfirmation(event.customerId(), event.orderId());
}
```

---

## Testing Transaction Behavior

### Unit Tests (No Transaction)

In unit tests, use a pass-through `TransactionRunner`:

```java
public class NoOpTransactionRunner implements TransactionRunner {

    @Override
    public void run(Runnable action) {
        action.run();
    }

    @Override
    public <T> T runReturning(Supplier<T> action) {
        return action.get();
    }
}
```

This runs the code directly without any transaction infrastructure, making unit tests fast and isolated.

### Integration Tests (Real Transaction)

Integration tests use the real `SpringTransactionRunner` with Testcontainers for a real PostgreSQL instance. The Spring test framework handles transaction rollback after each test:

```java
@SpringBootTest
@Transactional // Rolls back after each test
class CreateOrderCommandHandlerIntegrationTest {

    @Autowired
    private CreateOrderCommandHandler handler;

    @Test
    void should_create_order_successfully() {
        var command = new CreateOrderCommand(customerId, items);
        Result<OrderId> result = handler.handle(command);
        assertThat(result.isSuccess()).isTrue();
    }
}
```

---

## Rules Summary

| Rule | Rationale |
|------|-----------|
| `@Transactional` is **never** used in domain or application layers | Clean Architecture — no Spring dependency |
| `TransactionRunner` is the only transaction mechanism in application code | Explicit, testable, framework-free |
| One transaction per command handler | Atomic unit of work |
| Query handlers do not use transactions (unless read consistency required) | Reads are not transactional by default |
| Side-effect events use `AFTER_COMMIT` | Avoid side effects on rolled-back data |
| Use `NoOpTransactionRunner` in unit tests | Fast, isolated tests |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Using `@Transactional` on handlers or domain services | Use `TransactionRunner` |
| Forgetting to wrap handler logic in `transactionRunner.run()` | Always wrap write operations |
| Publishing events outside the transaction | Publish within `transactionRunner.run()` |
| Using `TransactionRunner` in query handlers | Queries don't need it (unless read consistency required) |
| Nested `transactionRunner.run()` calls | Avoid nesting; restructure to single transaction |
| Performing HTTP calls inside the transaction | Move external calls outside or to AFTER_COMMIT listeners |

---

## Related Documents

- [Patterns](patterns.md)
- [Repository Pattern](repository-pattern.md)
- [Domain Events](../domain/domain-events.md)
- [Backend Architecture](../architecture/backend-architecture.md)
