# Aggregates

## Purpose

This document defines the rules and patterns for implementing DDD aggregates in the CYNA Platform. Aggregates are the primary building blocks of the domain layer and serve as consistency boundaries for business invariants.

---

## What Is an Aggregate?

An **aggregate** is a cluster of domain objects (entities and value objects) treated as a single unit for the purpose of data changes. Every aggregate has:

- An **aggregate root** — the single entry point for all modifications.
- A **consistency boundary** — all invariants within the aggregate are guaranteed to be consistent after every operation.
- An **identity** — the aggregate root has a unique identifier.

---

## Aggregate Root Base Class

All aggregate roots extend the shared `AggregateRoot<ID>` base class:

```java
package com.cyna.shared.domain;

public abstract class AggregateRoot<ID> extends Entity<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected AggregateRoot(ID id) {
        super(id);
    }

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

---

## Entity Base Class

```java
package com.cyna.shared.domain;

public abstract class Entity<ID> {

    private final ID id;

    protected Entity(ID id) {
        if (id == null) throw new IllegalArgumentException("Entity ID must not be null");
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return id.equals(entity.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
```

---

## Value Object Guidelines

Value objects are immutable, identity-less objects defined entirely by their attributes.

```java
package com.cyna.shared.domain;

public abstract class ValueObject {

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
```

**Example — Money value object:**

```java
public record Money(BigDecimal amount, String currency) {

    public static final Money ZERO = new Money(BigDecimal.ZERO, "EUR");

    public Money {
        if (amount == null) throw new IllegalArgumentException("Amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Amount must not be negative");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("Currency must not be blank");
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }
}
```

**Example — Email value object:**

```java
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        value = value.toLowerCase().trim();
    }
}
```

**Example — Typed ID value object:**

```java
public record OrderId(UUID value) {

    public OrderId {
        if (value == null) throw new IllegalArgumentException("OrderId must not be null");
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }
}
```

---

## Aggregate Design Rules

### Rule 1: Protect All Invariants in the Aggregate Root

The aggregate root is the **only entry point** for state changes. All business rules are enforced in the root's methods, never by external callers.

```java
// ✅ CORRECT: Order enforces its own invariants
public class Order extends AggregateRoot<OrderId> {

    public Result<Void> addLine(ProductId productId, String productName, int quantity, Money unitPrice) {
        if (status != OrderStatus.DRAFT) {
            return Result.failure("Cannot add lines to a non-draft order");
        }
        if (quantity <= 0) {
            return Result.failure("Quantity must be positive");
        }
        if (lines.size() >= MAX_LINES) {
            return Result.failure("Order cannot have more than " + MAX_LINES + " lines");
        }

        this.lines.add(OrderLine.create(productId, productName, quantity, unitPrice));
        this.totalAmount = recalculateTotal();
        return Result.success();
    }
}
```

```java
// ❌ WRONG: Invariant enforcement outside the aggregate
if (order.getLines().size() < MAX_LINES) {
    order.getLines().add(new OrderLine(...)); // Bypasses aggregate root
}
```

### Rule 2: Use Private Constructors + Factory Methods

Never expose public constructors. Use static factory methods to create aggregates:

```java
public class User extends AggregateRoot<UserId> {

    // Private constructor — not accessible externally
    private User(UserId id, Email email, PasswordHash password, Role role) {
        super(id);
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = UserStatus.PENDING;
        this.createdAt = Instant.now();

        raise(new UserRegistered(id.value(), email.value(), role.name(), createdAt));
    }

    // Public factory method — the only way to create a User
    public static User register(Email email, PasswordHash password, Role role) {
        return new User(UserId.generate(), email, password, role);
    }
}
```

### Rule 3: Return Result, Not Exceptions

Business operations return `Result<T>` to communicate success or failure. Exceptions are reserved for programming errors and infrastructure failures.

```java
// ✅ CORRECT
public Result<Void> cancel(String reason) {
    if (status == OrderStatus.CANCELLED) {
        return Result.failure("Order is already cancelled");
    }
    if (status == OrderStatus.FULFILLED) {
        return Result.failure("Cannot cancel a fulfilled order");
    }
    this.status = OrderStatus.CANCELLED;
    this.cancellationReason = reason;
    raise(new OrderCancelled(getId().value(), reason, Instant.now()));
    return Result.success();
}

// ❌ WRONG: Throwing exception for a business rule violation
public void cancel(String reason) {
    if (status == OrderStatus.CANCELLED) {
        throw new IllegalStateException("Order is already cancelled");
    }
}
```

### Rule 4: Keep Aggregates Small

An aggregate should be as small as possible while still maintaining its invariants. Large aggregates lead to:

- Concurrency conflicts (more data locked during updates)
- Performance issues (more data loaded on every operation)
- Complexity (harder to understand and test)

**Guidelines:**

- Prefer referencing other aggregates by **ID**, not by object reference.
- Keep collections within an aggregate to a reasonable size.
- If a collection can grow unboundedly, consider making its items separate aggregates.

```java
// ✅ CORRECT: Reference by ID
public class Order extends AggregateRoot<OrderId> {
    private CustomerId customerId;  // Reference by ID, not Customer object
}

// ❌ WRONG: Object reference to another aggregate
public class Order extends AggregateRoot<OrderId> {
    private Customer customer;  // Creates coupling between aggregates
}
```

### Rule 5: One Transaction per Aggregate

A single transaction should modify **one aggregate only**. If you need to modify multiple aggregates in response to a single action, use **domain events** and eventual consistency.

```java
// ✅ CORRECT: Modify one aggregate, publish event for others
public Result<Void> handle(ConfirmOrderCommand command) {
    Order order = orderRepository.findById(command.orderId())
            .orElse(null);
    if (order == null) return Result.failure("Order not found");

    Result<Void> result = order.confirm();
    if (result.isFailure()) return result;

    orderRepository.save(order);
    // OrderConfirmed event will be handled by Payment module asynchronously
    return Result.success();
}

// ❌ WRONG: Modifying two aggregates in one transaction
public Result<Void> handle(ConfirmOrderCommand command) {
    Order order = orderRepository.findById(command.orderId());
    Payment payment = paymentRepository.create(order);
    // Two aggregates modified — violates transaction boundary rule
}
```

### Rule 6: Domain Layer Must Be Framework-Free

Aggregate root classes must not contain:

- `@Entity`, `@Table`, `@Column` (JPA annotations)
- `@NotNull`, `@Size`, `@Valid` (Jakarta Validation annotations)
- `@Component`, `@Service` (Spring annotations)
- Any import from `org.springframework.*`, `jakarta.*`, or `javax.*`

Validation is done through **Guard clauses** and **self-validating constructors**.

### Rule 7: Collections Are Immutable from the Outside

Internal collections exposed by getters must be unmodifiable:

```java
public List<OrderLine> getLines() {
    return Collections.unmodifiableList(lines);
}
```

---

## Guard Class

A utility class for precondition checks:

```java
package com.cyna.shared.domain;

public final class Guard {

    private Guard() {}

    public static void againstNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    public static void againstBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    public static void againstEmpty(Collection<?> value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }

    public static void againstNegativeOrZero(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    public static void againstLengthExceeding(String value, int maxLength, String name) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
        }
    }
}
```

---

## Aggregate Lifecycle Summary

```
             create()
                │
                ▼
          ┌───────────┐
          │  CREATED  │ ← Invariants checked, event raised
          └─────┬─────┘
                │
                │  modify() / transition()
                ▼
          ┌───────────┐
          │  UPDATED  │ ← Invariants re-checked, events raised
          └─────┬─────┘
                │
                │  save()
                ▼
          ┌───────────┐
          │ PERSISTED │ ← JPA mapper converts to JPA entity
          └─────┬─────┘
                │
                │  publishEvents()
                ▼
          ┌───────────┐
          │ EVENTS    │ ← Domain events dispatched to listeners
          │ PUBLISHED │
          └───────────┘
```

---

## Common Mistakes

| Mistake | Why It's Wrong | Fix |
|---------|---------------|-----|
| Anemic domain model (getters/setters only) | No invariant protection, logic leaks to services | Put business logic in aggregate methods |
| Public setters on aggregate properties | Anyone can bypass invariants | Use methods that enforce rules |
| JPA annotations on domain entities | Couples domain to persistence | Separate JPA entities in infrastructure |
| Throwing exceptions for business rules | Hides error paths, makes code unpredictable | Return `Result<T>` |
| Modifying multiple aggregates in one transaction | Violates consistency boundary | Use domain events for cross-aggregate updates |
| Unbounded collections in aggregates | Performance and complexity problems | Reference by ID, paginate, or split aggregate |

---

## Related Documents

- [Bounded Contexts](bounded-contexts.md)
- [Domain Events](domain-events.md)
- [Result Pattern](../development/result-pattern.md)
- [Repository Pattern](../development/repository-pattern.md)
