# Domain Events

## Purpose

This document defines the rules, conventions, and implementation patterns for domain events in the CYNA Platform. Domain events enable loose coupling between modules and are a core mechanism for inter-module communication.

---

## What Is a Domain Event?

A **domain event** is an immutable record of something significant that happened in the domain. Events describe **facts** — they are named in the past tense and cannot be rejected by subscribers.

Examples:
- `UserRegistered` — a new user completed registration
- `OrderConfirmed` — an order was confirmed by the system
- `PaymentSucceeded` — a payment was successfully processed

---

## Domain Event Base Interface

```java
package com.cyna.shared.domain;

public interface DomainEvent {
    Instant occurredAt();
}
```

All domain events implement this interface. Using Java records is the preferred implementation style.

---

## Naming Conventions

| Convention | Example |
|-----------|---------|
| Past tense verb | `OrderCreated`, not `CreateOrder` |
| Module prefix (implicit from package) | `com.cyna.order.domain.event.OrderCreated` |
| Describes what happened, not what to do | `PaymentSucceeded`, not `SendReceipt` |
| Specific and unambiguous | `OrderLineAdded`, not `OrderChanged` |

---

## Event Structure

Every domain event must include:

| Field | Type | Purpose |
|-------|------|---------|
| Aggregate ID | `UUID` | Identifies which aggregate produced the event |
| `occurredAt` | `Instant` | Timestamp when the event occurred |
| Relevant data | Primitives/UUIDs | Data subscribers need to react |

### Example Events

```java
package com.cyna.order.domain.event;

public record OrderCreated(
    UUID orderId,
    UUID customerId,
    BigDecimal totalAmount,
    String currency,
    Instant occurredAt
) implements DomainEvent {}
```

```java
package com.cyna.order.domain.event;

public record OrderCancelled(
    UUID orderId,
    String reason,
    Instant occurredAt
) implements DomainEvent {}
```

```java
package com.cyna.user.domain.event;

public record UserRegistered(
    UUID userId,
    String email,
    String role,
    Instant occurredAt
) implements DomainEvent {}
```

```java
package com.cyna.payment.domain.event;

public record PaymentSucceeded(
    UUID paymentId,
    UUID orderId,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    Instant occurredAt
) implements DomainEvent {}
```

---

## Event Lifecycle

### 1. Raising Events

Events are raised **inside the aggregate root** when a state change occurs:

```java
public class Order extends AggregateRoot<OrderId> {

    public static Order create(CustomerId customerId, List<OrderLine> lines) {
        Money total = calculateTotal(lines);
        var order = new Order(OrderId.generate(), customerId, lines, total);
        // Raise event during creation
        order.raise(new OrderCreated(
                order.getId().value(),
                customerId.value(),
                total.amount(),
                total.currency(),
                Instant.now()
        ));
        return order;
    }

    public Result<Void> confirm() {
        if (status != OrderStatus.PENDING) {
            return Result.failure("Order must be PENDING to confirm");
        }
        this.status = OrderStatus.CONFIRMED;
        // Raise event during state transition
        raise(new OrderConfirmed(getId().value(), Instant.now()));
        return Result.success();
    }
}
```

**Rule**: Events are collected in the aggregate root's internal list. They are **not** published immediately.

### 2. Collecting Events

The `AggregateRoot` base class accumulates events:

```java
public abstract class AggregateRoot<ID> extends Entity<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void raise(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
```

### 3. Publishing Events

Events are published **after the aggregate is persisted**, typically in the command handler or repository adapter:

```java
public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, OrderId> {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Result<OrderId> handle(CreateOrderCommand command) {
        Order order = Order.create(command.customerId(), command.lines());

        orderRepository.save(order);
        eventPublisher.publishAll(order.getDomainEvents());
        order.clearDomainEvents();

        return Result.success(order.getId());
    }
}
```

### 4. Publishing Infrastructure

The `DomainEventPublisher` uses Spring's `ApplicationEventPublisher` under the hood:

```java
package com.cyna.shared.infrastructure.event;

@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
```

### 5. Handling Events

Event handlers react to events from other modules:

**Application layer handler:**

```java
package com.cyna.notification.application.eventhandler;

public class OnUserRegisteredHandler {

    private final EmailService emailService;

    public OnUserRegisteredHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    public void handle(UserRegistered event) {
        emailService.sendWelcomeEmail(event.email(), event.userId());
    }
}
```

**Spring event listener (interfaces layer):**

```java
package com.cyna.notification.interfaces.eventlistener;

@Component
public class UserRegisteredListener {

    private final OnUserRegisteredHandler handler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegistered event) {
        handler.handle(event);
    }
}
```

---

## Transaction Boundaries

### Side-Effect Events (AFTER_COMMIT)

For events that trigger external side effects (sending emails, calling APIs), use `AFTER_COMMIT`:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(OrderConfirmed event) {
    // Only executed if the order was actually persisted
    notificationHandler.handle(event);
}
```

**Why?** If the transaction rolls back, the email or API call should not happen.

### Data-Consistency Events (BEFORE_COMMIT or synchronous)

For events that must update data within the same transaction, use synchronous `@EventListener` or `BEFORE_COMMIT`:

```java
@EventListener
public void on(OrderCreated event) {
    // Update read model within the same transaction
    orderSummaryProjection.project(event);
}
```

---

## Event Catalog

### User Module Events

| Event | Trigger | Data |
|-------|---------|------|
| `UserRegistered` | New user registers | userId, email, role, occurredAt |
| `UserActivated` | Admin activates user | userId, occurredAt |
| `UserDeactivated` | Admin deactivates user | userId, reason, occurredAt |
| `PasswordChanged` | User changes password | userId, occurredAt |

### Product Module Events

| Event | Trigger | Data |
|-------|---------|------|
| `ProductCreated` | Admin creates product | productId, name, price, occurredAt |
| `ProductPublished` | Admin publishes product | productId, occurredAt |
| `ProductUnpublished` | Admin unpublishes product | productId, occurredAt |
| `PriceChanged` | Admin changes price | productId, oldPrice, newPrice, occurredAt |

### Order Module Events

| Event | Trigger | Data |
|-------|---------|------|
| `OrderCreated` | Customer creates order | orderId, customerId, totalAmount, occurredAt |
| `OrderConfirmed` | System confirms order | orderId, occurredAt |
| `OrderPaid` | Payment succeeds | orderId, paymentId, occurredAt |
| `OrderFulfilled` | Services activated | orderId, occurredAt |
| `OrderCancelled` | Customer/admin cancels | orderId, reason, occurredAt |

### Payment Module Events

| Event | Trigger | Data |
|-------|---------|------|
| `PaymentInitiated` | Payment process starts | paymentId, orderId, amount, occurredAt |
| `PaymentSucceeded` | Payment provider confirms | paymentId, orderId, amount, occurredAt |
| `PaymentFailed` | Payment provider rejects | paymentId, orderId, reason, occurredAt |
| `RefundCompleted` | Refund processed | paymentId, orderId, amount, occurredAt |

---

## Rules

1. **Events are immutable.** Use Java records. No setters.
2. **Events carry only primitives and value types.** Never include domain objects or JPA entities.
3. **Events are self-contained.** A subscriber must not need to query the publisher to understand the event.
4. **Events describe facts, not intents.** Name them in the past tense.
5. **One event per state transition.** Each meaningful change raises exactly one event.
6. **Publish after persistence.** Never publish events before the aggregate is saved.
7. **Side effects use AFTER_COMMIT.** Email, API calls, etc., must not execute if the transaction rolls back.
8. **Handlers must be idempotent.** In case of retry or redelivery, handling the same event twice must not cause incorrect state.

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Publishing events before saving the aggregate | Always save first, then publish |
| Including domain objects in events | Use primitive types and UUIDs only |
| Naming events as commands (`SendEmail`) | Name as facts (`EmailSent`) |
| Creating handlers with side effects inside `@EventListener` | Use `@TransactionalEventListener(AFTER_COMMIT)` |
| Non-idempotent handlers | Add idempotency checks (e.g., check if already processed) |
| Fat events with all aggregate data | Include only what subscribers need |

---

## Future: Outbox Pattern

When the system evolves toward microservices or needs guaranteed event delivery, the **Outbox Pattern** should be introduced:

1. Events are stored in an `outbox` table within the same transaction as the aggregate.
2. A background worker reads the outbox and publishes events to a message broker (e.g., Kafka, RabbitMQ).
3. Published events are marked as processed.

This guarantees **at-least-once delivery** and **transactional consistency**.

---

## Related Documents

- [Aggregates](aggregates.md)
- [Bounded Contexts](bounded-contexts.md)
- [Inter-Module Communication](../architecture/inter-module-communication.md)
- [Transaction Management](../development/transaction-management.md)
