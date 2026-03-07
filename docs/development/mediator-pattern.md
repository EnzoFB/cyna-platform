# Mediator Pattern

## Purpose

This document describes the Mediator pattern implementation in the CYNA Platform. The Mediator decouples HTTP controllers from application-layer command and query handlers, acting as a central dispatcher.

---

## Overview

The Mediator pattern provides a single entry point for dispatching commands and queries. Instead of controllers directly instantiating or injecting specific handlers, they delegate to the Mediator, which resolves and invokes the correct handler.

```
Controller → Mediator → CommandHandler / QueryHandler
```

### Benefits

- **Decoupling**: Controllers depend only on `Mediator`, not on individual handlers.
- **Consistency**: Every use case follows the same dispatch pattern.
- **Cross-cutting concerns**: Logging, validation, and transaction management can be applied uniformly via Mediator decorators/pipelines.
- **Testability**: Controllers and handlers can be tested independently.

---

## Core Interfaces

### Command

```java
package com.cyna.shared.application;

/**
 * Marker interface for commands.
 * A command represents an intent to change the system's state.
 *
 * @param <R> the type of the result value on success
 */
public interface Command<R> {}
```

### Query

```java
package com.cyna.shared.application;

/**
 * Marker interface for queries.
 * A query represents an intent to read data without modifying state.
 *
 * @param <R> the type of the result value
 */
public interface Query<R> {}
```

### CommandHandler

```java
package com.cyna.shared.application;

/**
 * Handles a command and returns a Result.
 *
 * @param <C> the command type
 * @param <R> the success value type
 */
public interface CommandHandler<C extends Command<R>, R> {
    Result<R> handle(C command);
}
```

### QueryHandler

```java
package com.cyna.shared.application;

/**
 * Handles a query and returns the result directly.
 *
 * @param <Q> the query type
 * @param <R> the result type
 */
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
```

### Mediator

```java
package com.cyna.shared.application;

/**
 * Central dispatcher for commands and queries.
 */
public interface Mediator {

    /**
     * Dispatches a command to its handler.
     *
     * @param command the command to execute
     * @param <R>     the success result type
     * @return the result of the command execution
     */
    <R> Result<R> send(Command<R> command);

    /**
     * Dispatches a query to its handler.
     *
     * @param query the query to execute
     * @param <R>   the result type
     * @return the result of the query execution
     */
    <R> R send(Query<R> query);
}
```

---

## Implementation

### Spring-Based Mediator

The infrastructure layer provides a Spring-based implementation that resolves handlers from the application context:

```java
package com.cyna.shared.infrastructure.mediator;

@Component
public class SpringMediator implements Mediator {

    private final ApplicationContext applicationContext;

    public SpringMediator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Result<R> send(Command<R> command) {
        CommandHandler<Command<R>, R> handler = resolveCommandHandler(command);
        return handler.handle(command);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> query) {
        QueryHandler<Query<R>, R> handler = resolveQueryHandler(query);
        return handler.handle(query);
    }

    @SuppressWarnings("unchecked")
    private <R> CommandHandler<Command<R>, R> resolveCommandHandler(Command<R> command) {
        String handlerName = command.getClass().getSimpleName() + "Handler";
        return (CommandHandler<Command<R>, R>) applicationContext
                .getBeansOfType(CommandHandler.class)
                .values()
                .stream()
                .filter(h -> h.getClass().getSimpleName().equals(handlerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No handler found for command: " + command.getClass().getSimpleName()));
    }

    @SuppressWarnings("unchecked")
    private <R> QueryHandler<Query<R>, R> resolveQueryHandler(Query<R> query) {
        String handlerName = query.getClass().getSimpleName() + "Handler";
        return (QueryHandler<Query<R>, R>) applicationContext
                .getBeansOfType(QueryHandler.class)
                .values()
                .stream()
                .filter(h -> h.getClass().getSimpleName().equals(handlerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No handler found for query: " + query.getClass().getSimpleName()));
    }
}
```

### Handler Registration

Handlers are registered as Spring components and automatically discovered:

```java
@Component
public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, OrderId> {

    private final OrderRepository orderRepository;

    public CreateOrderCommandHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Result<OrderId> handle(CreateOrderCommand command) {
        // ... business logic
    }
}
```

---

## Usage in Controllers

Controllers inject only the `Mediator` and delegate all business logic:

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final Mediator mediator;

    public OrderController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var command = new CreateOrderCommand(
                new CustomerId(request.customerId()),
                request.items().stream()
                        .map(i -> new OrderItemDto(i.productId(), i.quantity()))
                        .toList()
        );

        Result<OrderId> result = mediator.send(command);

        return result.fold(
                orderId -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(orderId.value())),
                error -> ResponseEntity.badRequest()
                        .body(ApiResponse.error(error))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderReadModel>> getOrderById(@PathVariable UUID id) {
        var query = new GetOrderByIdQuery(OrderId.of(id));
        OrderReadModel order = mediator.send(query);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
```

---

## Pipeline Behaviors (Cross-Cutting Concerns)

The Mediator can be extended with pipeline behaviors — decorators that execute before/after handlers:

### Logging Pipeline

```java
@Component
@Order(1)
public class LoggingPipeline {

    private static final Logger log = LoggerFactory.getLogger(LoggingPipeline.class);

    public <R> Result<R> handle(Command<R> command, CommandHandler<Command<R>, R> next) {
        log.info("Executing command: {}", command.getClass().getSimpleName());
        Instant start = Instant.now();

        Result<R> result = next.handle(command);

        Duration duration = Duration.between(start, Instant.now());
        log.info("Command {} completed in {}ms — success: {}",
                command.getClass().getSimpleName(),
                duration.toMillis(),
                result.isSuccess());

        return result;
    }
}
```

### Validation Pipeline

```java
@Component
@Order(2)
public class ValidationPipeline {

    public <R> Result<R> handle(Command<R> command, CommandHandler<Command<R>, R> next) {
        // Validate command before execution
        List<String> violations = validate(command);
        if (!violations.isEmpty()) {
            return Result.failure("Validation failed: " + String.join(", ", violations));
        }
        return next.handle(command);
    }
}
```

---

## Rules

| Rule | Rationale |
|------|-----------|
| Controllers must only use `Mediator` to dispatch commands/queries | Decoupling |
| One handler per command/query | Single Responsibility |
| Handler class name = Command/Query class name + "Handler" | Auto-resolution convention |
| Commands return `Result<T>` | Explicit error handling |
| Queries return the read model directly (no `Result` wrapper) | Queries should not fail with business errors |
| Handlers must be stateless | Thread safety |
| Handlers are Spring `@Component` beans | Auto-discovery |
| No business logic in controllers | Controllers are thin adapters |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Controller injects handler directly instead of Mediator | Inject `Mediator` only |
| Multiple commands in a single handler | One handler per command |
| Handler named differently from convention | Name must be `{CommandName}Handler` |
| Business logic in the controller | Move to command handler |
| Query handler modifying data | Queries must be read-only |

---

## Related Documents

- [Patterns](patterns.md)
- [Result Pattern](result-pattern.md)
- [Backend Architecture](../architecture/backend-architecture.md)
