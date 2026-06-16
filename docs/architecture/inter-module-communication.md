# Inter-Module Communication

## Purpose

This document defines how modules communicate with each other in the CYNA Platform backend. Proper inter-module communication is essential for maintaining module isolation and enabling future microservice extraction.

---

## Fundamental Rules

1. **Modules must not share database tables.** Each module owns its schema exclusively.
2. **Modules must not import each other's domain or infrastructure layers.**
3. **Modules communicate only through two mechanisms:**
   - **Synchronous**: Application-layer public API interfaces
   - **Asynchronous**: Domain events

---

## Mechanism 1: Public API Interfaces (Synchronous)

### What It Is

A module exposes a **public API interface** in its `application.api` package. Other modules depend on this interface to request data or trigger operations. The implementation lives in the owning module's application or infrastructure layer.

### When to Use

- When Module A needs **data** from Module B to complete a use case.
- When the calling module needs a **synchronous response**.
- For **queries** across module boundaries.

### Structure

**Module B (provider):**

```java
// com.cyna.product.application.api.ProductQueryApi
public interface ProductQueryApi {
    Optional<ProductInfo> getById(ProductId productId);
    List<ProductInfo> getByIds(List<ProductId> productIds);
    boolean exists(ProductId productId);
}

// com.cyna.product.application.api.ProductInfo (read-only DTO)
public record ProductInfo(
    UUID id,
    String name,
    String description,
    BigDecimal price,
    String currency
) {}
```

**Module B (implementation):**

```java
// com.cyna.product.application.api.ProductQueryApiImpl
@Service
class ProductQueryApiImpl implements ProductQueryApi {

    private final ProductRepository productRepository;

    ProductQueryApiImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<ProductInfo> getById(ProductId productId) {
        return productRepository.findById(productId)
                .map(product -> new ProductInfo(
                        product.getId().value(),
                        product.getName().value(),
                        product.getDescription(),
                        product.getPrice().amount(),
                        product.getPrice().currency()
                ));
    }

    @Override
    public List<ProductInfo> getByIds(List<ProductId> productIds) {
        return productRepository.findAllByIds(productIds).stream()
                .map(product -> new ProductInfo(
                        product.getId().value(),
                        product.getName().value(),
                        product.getDescription(),
                        product.getPrice().amount(),
                        product.getPrice().currency()
                ))
                .toList();
    }

    @Override
    public boolean exists(ProductId productId) {
        return productRepository.existsById(productId);
    }
}
```

**Module A (consumer):**

```java
// com.cyna.order.application.command.create.CreateOrderCommandHandler
public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, OrderId> {

    private final OrderRepository orderRepository;
    private final ProductQueryApi productQueryApi; // Injected, not directly accessed

    @Override
    public Result<OrderId> handle(CreateOrderCommand command) {
        // Use the public API to get product information
        var productInfo = productQueryApi.getById(command.productId());
        if (productInfo.isEmpty()) {
            return Result.failure("Product not found");
        }
        // ... create order using product info
    }
}
```

### Rules for Public APIs

| Rule | Rationale |
|------|-----------|
| Return DTOs, never domain objects | Prevents domain coupling between modules |
| Keep the API surface minimal | Only expose what other modules actually need |
| Interface lives in `application.api` package | This is the only cross-module dependency allowed |
| Implementation is package-private | Consumers depend on the interface only |
| DTOs are immutable records | Safety and clarity |
| Use value types (not raw primitives) for IDs | Type safety across module boundaries |

### Microservice Migration Path

When extracting to microservices, the public API interface becomes a **client interface** backed by an HTTP/gRPC client instead of an in-process call. The consuming module's code does not change.

```
Monolith:    OrderModule → ProductQueryApi (in-process)
Microservice: OrderModule → ProductQueryApi → HTTP Client → Product Service
```

---

## Mechanism 2: Domain Events (Asynchronous)

### What It Is

A module publishes **domain events** when something significant happens. Other modules can subscribe to these events and react accordingly, without the publisher knowing about the subscribers.

### When to Use

- When Module B needs to **react** to something that happened in Module A.
- When the reaction is **eventually consistent** (no immediate response needed).
- For **side effects** like sending notifications, updating read models, or triggering workflows.

### Event Flow

```
Module A (Publisher)                    Module B (Subscriber)
┌──────────────────┐                  ┌──────────────────────────┐
│  Domain Layer    │                  │  Application Layer       │
│  Order.confirm() │                  │  OnOrderConfirmed        │
│  → raises event  │                  │  Handler                 │
│    OrderConfirmed│                  │                          │
└────────┬─────────┘                  └───────────▲──────────────┘
         │                                        │
         │  ┌────────────────────────────┐        │
         ─▶│  DomainEventPublisher       │───────┘
            │  (Spring ApplicationEvent) │
            └────────────────────────────┘
```

### Implementation

**Step 1: Define the event (publisher's domain layer)**

```java
// com.cyna.order.domain.event.OrderConfirmed
public record OrderConfirmed(
    UUID orderId,
    UUID customerId,
    BigDecimal totalAmount,
    Instant occurredAt
) implements DomainEvent {}
```

**Step 2: Raise the event in the aggregate**

```java
// com.cyna.order.domain.model.Order
public Result<Void> confirm() {
    if (status != OrderStatus.PENDING) {
        return Result.failure("Cannot confirm non-pending order");
    }
    this.status = OrderStatus.CONFIRMED;
    raise(new OrderConfirmed(getId().value(), customerId.value(), totalAmount.amount(), Instant.now()));
    return Result.success();
}
```

**Step 3: Publish events after persistence (infrastructure)**

```java
// In the command handler or TransactionRunner callback
Order order = orderRepository.findById(orderId).orElseThrow();
Result<Void> result = order.confirm();
if (result.isSuccess()) {
    orderRepository.save(order);
    domainEventPublisher.publishAll(order.getDomainEvents());
    order.clearDomainEvents();
}
```

**Step 4: Handle the event (subscriber's application layer)**

```java
// com.cyna.notification.application.eventhandler.OnOrderConfirmedHandler
public class OnOrderConfirmedHandler {

    private final NotificationService notificationService;

    public void handle(OrderConfirmed event) {
        notificationService.sendOrderConfirmationEmail(event.customerId(), event.orderId());
    }
}
```

**Step 5: Wire via Spring event listener (subscriber's interfaces layer)**

```java
// com.cyna.notification.interfaces.eventlistener.OrderConfirmedListener
@Component
public class OrderConfirmedListener {

    private final OnOrderConfirmedHandler handler;

    @EventListener
    public void on(OrderConfirmed event) {
        handler.handle(event);
    }
}
```

### Rules for Domain Events

| Rule | Rationale |
|------|-----------|
| Events are immutable records | Thread safety, predictability |
| Events carry only primitive/value data | Avoid coupling to domain objects |
| Events describe what happened, not what to do | Naming: `OrderConfirmed`, not `SendConfirmationEmail` |
| Events are named in past tense | Clarity of intent |
| Events must be self-contained | The subscriber must not need to query the publisher |
| Events include a timestamp (`occurredAt`) | Auditing and ordering |
| Events include the aggregate ID | Traceability |
| Use `@TransactionalEventListener(phase = AFTER_COMMIT)` for side effects | Avoid acting on uncommitted data |

### Transaction Boundaries

**Critical**: Domain events that trigger side effects (email, external API calls) must be processed **after the transaction commits**. Use Spring's `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(OrderConfirmed event) {
    handler.handle(event); // Email is sent only if the order was actually saved
}
```

For events that must update data within the same transaction, use `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)` or synchronous `@EventListener`.

---

## Anti-Patterns to Avoid

### 1. Direct Database Access Across Modules

```java
// ❌ WRONG: Order module querying Product table directly
@Query("SELECT p FROM ProductJpaEntity p WHERE p.id = :id")
ProductJpaEntity findProductById(UUID id);
```

```java
// ✅ CORRECT: Use the Product module's public API
ProductInfo product = productQueryApi.getById(productId);
```

### 2. Importing Another Module's Domain Classes

```java
// ❌ WRONG: Order module importing Product domain
import com.cyna.product.domain.model.Product;
```

```java
// ✅ CORRECT: Use the public API's DTO
import com.cyna.product.application.api.ProductInfo;
```

### 3. Circular Dependencies Between Modules

If Module A depends on Module B's API and Module B depends on Module A's API, you have a circular dependency. Solutions:

- **Extract a shared contract** into a common module.
- **Use domain events** instead of synchronous calls for one direction.
- **Reconsider module boundaries** — the modules may be too granular.

### 4. Fat Events

```java
// ❌ WRONG: Event carries the entire aggregate
public record OrderCreated(Order order) implements DomainEvent {}
```

```java
// ✅ CORRECT: Event carries only necessary data
public record OrderCreated(
    UUID orderId,
    UUID customerId,
    BigDecimal totalAmount,
    Instant occurredAt
) implements DomainEvent {}
```

---

## Decision Matrix

| Scenario | Mechanism | Rationale |
|----------|-----------|-----------|
| Module A needs data from Module B to complete a command | Public API (sync) | Immediate response required |
| Module A wants to notify others that something happened | Domain Event (async) | Loose coupling, no response needed |
| Module A needs to trigger a command in Module B | Domain Event (async) | Module B decides how to react |
| Module A needs to validate data in Module B | Public API (sync) | Validation requires synchronous check |
| Module A needs to update a read model based on Module B changes | Domain Event (async) | Eventually consistent is acceptable |

---

## Related Documents

- [Dependency Rules](dependency-rules.md)
- [Domain Events](../domain/domain-events.md)
- [Module Structure](module-structure.md)
