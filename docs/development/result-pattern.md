# Result Pattern

## Purpose

This document describes the Result pattern used in the CYNA Platform for explicit, composable error handling. The Result pattern replaces exceptions for business-level errors, making success and failure paths visible in the type system.

---

## Why Not Exceptions?

In traditional Java code, business rule violations are often communicated via exceptions:

```java
// ❌ Exception-based approach
public Order confirm() {
    if (status != OrderStatus.PENDING) {
        throw new IllegalStateException("Cannot confirm non-pending order");
    }
    // ...
}
```

Problems with this approach:

1. **Hidden control flow** — Callers cannot see from the method signature that it can fail.
2. **Exception-driven flow control** — Exceptions are meant for exceptional, unexpected failures, not for expected business outcomes.
3. **Unchecked exceptions** — Java doesn't force callers to handle unchecked exceptions.
4. **Performance overhead** — Exception creation captures a stack trace, which is expensive.
5. **Composability** — Exceptions don't compose well across multiple operations.

---

## The Result Type

### Definition

```java
package com.cyna.shared.domain;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    boolean isSuccess();
    boolean isFailure();
    T getValue();
    String getError();

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static Result<Void> success() {
        return new Success<>(null);
    }

    static <T> Result<T> failure(String error) {
        return new Failure<>(error);
    }

    /**
     * Transforms the result: applies onSuccess if successful, onFailure if failed.
     */
    <R> R fold(Function<T, R> onSuccess, Function<String, R> onFailure);

    /**
     * Maps the success value to a new type.
     */
    <R> Result<R> map(Function<T, R> mapper);

    /**
     * Chains another Result-returning operation.
     */
    <R> Result<R> flatMap(Function<T, Result<R>> mapper);

    // --- Implementations ---

    record Success<T>(T value) implements Result<T> {
        @Override public boolean isSuccess() { return true; }
        @Override public boolean isFailure() { return false; }
        @Override public T getValue() { return value; }
        @Override public String getError() { throw new UnsupportedOperationException("No error on success"); }

        @Override
        public <R> R fold(Function<T, R> onSuccess, Function<String, R> onFailure) {
            return onSuccess.apply(value);
        }

        @Override
        public <R> Result<R> map(Function<T, R> mapper) {
            return new Success<>(mapper.apply(value));
        }

        @Override
        public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
            return mapper.apply(value);
        }
    }

    record Failure<T>(String error) implements Result<T> {
        @Override public boolean isSuccess() { return false; }
        @Override public boolean isFailure() { return true; }
        @Override public T getValue() { throw new UnsupportedOperationException("No value on failure"); }
        @Override public String getError() { return error; }

        @Override
        public <R> R fold(Function<T, R> onSuccess, Function<String, R> onFailure) {
            return onFailure.apply(error);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R> map(Function<T, R> mapper) {
            return (Result<R>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
            return (Result<R>) this;
        }
    }
}
```

---

## Usage

### In Domain Layer

Domain methods return `Result<T>` to communicate business rule outcomes:

```java
public class Order extends AggregateRoot<OrderId> {

    public Result<Void> confirm() {
        if (status != OrderStatus.PENDING) {
            return Result.failure("Order can only be confirmed when PENDING. Current status: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
        raise(new OrderConfirmed(getId().value(), Instant.now()));
        return Result.success();
    }

    public Result<Void> addLine(ProductId productId, String name, int quantity, Money unitPrice) {
        if (status != OrderStatus.DRAFT) {
            return Result.failure("Cannot add lines to a non-draft order");
        }
        if (quantity <= 0) {
            return Result.failure("Quantity must be positive");
        }
        if (lines.size() >= MAX_LINES) {
            return Result.failure("Order cannot have more than " + MAX_LINES + " lines");
        }

        lines.add(OrderLine.create(productId, name, quantity, unitPrice));
        totalAmount = recalculateTotal();
        return Result.success();
    }
}
```

### In Application Layer (Command Handlers)

Command handlers return `Result<T>`:

```java
public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, OrderId> {

    @Override
    public Result<OrderId> handle(CreateOrderCommand command) {
        // Validate inputs
        if (command.items().isEmpty()) {
            return Result.failure("Order must contain at least one item");
        }

        var customer = customerQueryApi.getById(command.customerId());
        if (customer.isEmpty()) {
            return Result.failure("Customer not found: " + command.customerId());
        }

        // Create aggregate
        Order order = Order.create(command.customerId(), buildLines(command.items()));
        orderRepository.save(order);
        eventPublisher.publishAll(order.getDomainEvents());

        return Result.success(order.getId());
    }
}
```

### In Interfaces Layer (Controllers)

Controllers use `fold()` to map Results to HTTP responses:

```java
@PostMapping
public ResponseEntity<ApiResponse<UUID>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    var command = mapToCommand(request);
    Result<OrderId> result = mediator.send(command);

    return result.fold(
            orderId -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(orderId.value())),
            error -> ResponseEntity.badRequest()
                    .body(ApiResponse.error(error))
    );
}
```

### Composing Results

Use `flatMap` to chain operations that each return a `Result`:

```java
public Result<OrderId> handle(CreateOrderCommand command) {
    return validateCustomer(command.customerId())
            .flatMap(customer -> validateProducts(command.items()))
            .flatMap(products -> createOrder(command, products))
            .map(Order::getId);
}

private Result<CustomerInfo> validateCustomer(CustomerId customerId) {
    return customerQueryApi.getById(customerId)
            .map(Result::success)
            .orElse(Result.failure("Customer not found"));
}
```

Use `map` to transform the success value:

```java
Result<UUID> orderUuid = createOrderResult.map(orderId -> orderId.value());
```

---

## When to Use Result vs Exceptions

| Scenario | Use | Rationale |
|----------|-----|-----------|
| Business rule violation | `Result.failure()` | Expected outcome, caller must handle |
| Entity not found | `Result.failure()` | Expected outcome in business flow |
| Invalid command data | `Result.failure()` | Input validation in application layer |
| Database connection lost | Throw exception | Unexpected infrastructure failure |
| Null pointer (bug) | Throw exception | Programming error |
| Out of memory | Throw exception | JVM-level failure |
| Aggregate invariant in constructor | Throw `IllegalArgumentException` | Programming error — should never happen if application validates first |

**Rule of thumb**: If the caller is expected to handle the failure case as part of normal business flow, use `Result`. If the failure indicates a bug or infrastructure problem, throw an exception.

---

## Error Codes (Optional Enhancement)

For more structured errors, extend the failure case with error codes:

```java
public record AppError(String code, String message) {
    public static AppError notFound(String entity, Object id) {
        return new AppError("NOT_FOUND", entity + " not found: " + id);
    }

    public static AppError validationFailed(String message) {
        return new AppError("VALIDATION_FAILED", message);
    }

    public static AppError businessRule(String message) {
        return new AppError("BUSINESS_RULE_VIOLATION", message);
    }
}
```

Then use `Result<T>` with `AppError` instead of raw `String`:

```java
public sealed interface Result<T> {
    static <T> Result<T> failure(AppError error) { ... }
    AppError getError();
}
```

---

## Rules

| Rule | Rationale |
|------|-----------|
| All command handlers return `Result<T>` | Consistent, explicit error handling |
| Domain methods return `Result<T>` for operations that can fail in a business sense | Explicit failure paths |
| Never call `getValue()` without checking `isSuccess()` first | Avoid `UnsupportedOperationException` |
| Prefer `fold()` over `if/else` on `isSuccess()` | Functional style, no chance of forgetting a branch |
| Use `flatMap()` for chaining Result-returning operations | Composability |
| Error messages must be user-meaningful | Not "error#42" but "Order cannot be confirmed" |
| Exceptions are for bugs and infrastructure failures only | Clear separation of concerns |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Throwing exceptions for business rules | Return `Result.failure()` |
| Ignoring the failure case | Always handle both paths with `fold()` |
| Calling `getValue()` without checking success | Use `fold()` or check `isSuccess()` first |
| Using `Result` for infrastructure errors | Throw exceptions for infrastructure failures |
| Returning `null` instead of `Result.failure()` | Always use `Result` |
| Overly generic error messages ("An error occurred") | Be specific: "Order must be PENDING to confirm" |

---

## Related Documents

- [Patterns](patterns.md)
- [Error Handling](../api/error-handling.md)
- [Mediator Pattern](mediator-pattern.md)
- [Aggregates](../domain/aggregates.md)
