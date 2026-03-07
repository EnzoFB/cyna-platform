# Backend Architecture

## Purpose

This document details the internal architecture of the CYNA Platform backend: a Java 21 / Spring Boot 3.x **Modular Monolith** following **Clean Architecture** and **Domain-Driven Design**.

---

## Project Structure

```
cyna-backend/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
├── shared/                          # Shared kernel (common abstractions)
│   ├── domain/
│   │   ├── Result.java
│   │   ├── DomainEvent.java
│   │   ├── AggregateRoot.java
│   │   ├── Entity.java
│   │   ├── ValueObject.java
│   │   └── ...
│   ├── application/
│   │   ├── Command.java
│   │   ├── Query.java
│   │   ├── CommandHandler.java
│   │   ├── QueryHandler.java
│   │   ├── Mediator.java
│   │   └── TransactionRunner.java
│   └── infrastructure/
│       ├── security/
│       └── persistence/
│
├── modules/
│   ├── user/                        # User management module
│   │   ├── domain/
│   │   ├── application/
│   │   ├── interfaces/
│   │   └── infrastructure/
│   │
│   ├── product/                     # Product catalog module
│   │   ├── domain/
│   │   ├── application/
│   │   ├── interfaces/
│   │   └── infrastructure/
│   │
│   ├── order/                       # Order management module
│   │   ├── domain/
│   │   ├── application/
│   │   ├── interfaces/
│   │   └── infrastructure/
│   │
│   └── payment/                     # Payment processing module
│       ├── domain/
│       ├── application/
│       ├── interfaces/
│       └── infrastructure/
│
└── app/                             # Application bootstrap
    └── CynaApplication.java
```

---

## Clean Architecture Layers

Each module is organized into four layers with strict dependency rules.

### Layer 1: Domain

**Package**: `com.cyna.{module}.domain`

The domain layer is the core of the module. It contains:

- **Aggregates** — Cluster of entities with a single root governing invariants.
- **Entities** — Objects with identity and lifecycle.
- **Value Objects** — Immutable objects defined by their attributes.
- **Domain Events** — Records of things that happened in the domain.
- **Repository Interfaces** — Port definitions for data access.
- **Domain Services** — Stateless operations that span multiple aggregates.

**Rules:**

| Rule | Enforcement |
|------|-------------|
| No Spring annotations | ArchUnit |
| No JPA annotations | ArchUnit |
| No Jakarta Validation annotations | ArchUnit |
| No framework imports | ArchUnit |
| All domain objects are immutable or self-validating | Code review |
| Aggregate roots enforce all invariants | Code review |
| Value objects override `equals()` and `hashCode()` | Code review |
| Domain events are immutable records | Code review |

**Example — Aggregate Root:**

```java
package com.cyna.order.domain;

public class Order extends AggregateRoot<OrderId> {

    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderLine> lines;
    private final Money totalAmount;
    private final Instant createdAt;

    private Order(OrderId id, CustomerId customerId, List<OrderLine> lines, Money totalAmount) {
        super(id);
        Guard.againstNull(customerId, "customerId");
        Guard.againstEmpty(lines, "lines");
        Guard.againstNegativeOrZero(totalAmount.amount(), "totalAmount");

        this.customerId = customerId;
        this.status = OrderStatus.PENDING;
        this.lines = List.copyOf(lines);
        this.totalAmount = totalAmount;
        this.createdAt = Instant.now();

        raise(new OrderCreated(id, customerId, totalAmount, createdAt));
    }

    public static Order create(CustomerId customerId, List<OrderLine> lines) {
        Money total = lines.stream()
                .map(OrderLine::subtotal)
                .reduce(Money.ZERO, Money::add);
        return new Order(OrderId.generate(), customerId, lines, total);
    }

    public Result<Void> confirm() {
        if (status != OrderStatus.PENDING) {
            return Result.failure("Order can only be confirmed when PENDING");
        }
        this.status = OrderStatus.CONFIRMED;
        raise(new OrderConfirmed(getId(), Instant.now()));
        return Result.success();
    }
}
```

### Layer 2: Application

**Package**: `com.cyna.{module}.application`

The application layer orchestrates use cases. It contains:

- **Commands** — Intent to change state.
- **Command Handlers** — Execute commands, coordinate domain objects.
- **Queries** — Intent to read data.
- **Query Handlers** — Execute queries, return read models.
- **Application Services** — Orchestration logic used by multiple handlers.
- **Port Interfaces** — Interfaces for external services (email, payment gateway).
- **Public API Interfaces** — Contracts exposed to other modules.

**Rules:**

| Rule | Enforcement |
|------|-------------|
| One handler per command/query | Code review |
| Handlers must not contain business logic | Code review |
| Handlers coordinate domain objects and call repositories | Code review |
| No direct HTTP or persistence concerns | ArchUnit |
| Commands return `Result<T>` | Code review |
| Queries return read models (DTOs), not entities | Code review |

**Example — Command Handler:**

```java
package com.cyna.order.application.commands.create;

public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, OrderId> {

    private final OrderRepository orderRepository;
    private final ProductQueryApi productQueryApi; // public API from Product module

    public CreateOrderCommandHandler(OrderRepository orderRepository,
                                     ProductQueryApi productQueryApi) {
        this.orderRepository = orderRepository;
        this.productQueryApi = productQueryApi;
    }

    @Override
    public Result<OrderId> handle(CreateOrderCommand command) {
        List<OrderLine> lines = command.items().stream()
                .map(item -> {
                    var product = productQueryApi.getById(item.productId());
                    return OrderLine.create(product.id(), product.name(), item.quantity(), product.price());
                })
                .toList();

        Order order = Order.create(command.customerId(), lines);
        orderRepository.save(order);

        return Result.success(order.getId());
    }
}
```

### Layer 3: Interfaces

**Package**: `com.cyna.{module}.interfaces`

The interfaces layer adapts external inputs to application use cases. It contains:

- **REST Controllers** — HTTP endpoints.
- **Request DTOs** — Objects representing HTTP request bodies.
- **Response DTOs** — Objects representing HTTP response bodies.
- **Mappers** — Transform between DTOs and application-layer objects.
- **Validation** — Jakarta Validation annotations on request DTOs (NOT on domain).

**Rules:**

| Rule | Enforcement |
|------|-------------|
| Controllers only delegate to Mediator | Code review |
| Controllers must not contain business logic | Code review |
| Jakarta Validation is allowed here (and only here) | ArchUnit |
| Response DTOs never expose domain entities directly | Code review |
| HTTP status codes follow REST conventions | Code review |

**Example — Controller:**

```java
package com.cyna.order.interfaces.rest;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final Mediator mediator;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderIdResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        var command = new CreateOrderCommand(
                request.customerId(),
                request.items().stream()
                        .map(i -> new OrderItemDto(i.productId(), i.quantity()))
                        .toList()
        );

        Result<OrderId> result = mediator.send(command);

        return result.fold(
                orderId -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(new OrderIdResponse(orderId.value()))),
                error -> ResponseEntity.badRequest()
                        .body(ApiResponse.error(error))
        );
    }
}
```

### Layer 4: Infrastructure

**Package**: `com.cyna.{module}.infrastructure`

The infrastructure layer implements all technical adapters. It contains:

- **JPA Entities** — ORM-mapped data classes (distinct from domain entities).
- **JPA Repository Adapters** — Implement domain repository interfaces using JPA.
- **JPA Mappers** — Convert between domain objects and JPA entities.
- **External Service Adapters** — Implement application port interfaces.
- **Configuration** — Spring beans, module-specific config.

**Rules:**

| Rule | Enforcement |
|------|-------------|
| JPA entities are separate from domain entities | ArchUnit |
| Adapters implement domain/application interfaces | Code review |
| No business logic in infrastructure | Code review |
| Spring annotations allowed here | — |
| JPA annotations allowed here | — |
| Each module has its own database schema | Migration review |

**Example — JPA Repository Adapter:**

```java
package com.cyna.order.infrastructure.persistence;

@Repository
@RequiredArgsConstructor
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository springRepo;
    private final OrderJpaMapper mapper;

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
}
```

---

## Shared Kernel

The `shared/` module contains abstractions and building blocks used across all modules:

| Component | Layer | Purpose |
|-----------|-------|---------|
| `AggregateRoot<ID>` | domain | Base class for aggregate roots; holds domain events |
| `Entity<ID>` | domain | Base class for entities |
| `ValueObject` | domain | Base class for value objects |
| `DomainEvent` | domain | Marker interface for domain events |
| `Result<T>` | domain | Result monad for explicit error handling |
| `Guard` | domain | Precondition utility class |
| `Command<R>` | application | Marker for commands |
| `Query<R>` | application | Marker for queries |
| `CommandHandler<C, R>` | application | Handler interface for commands |
| `QueryHandler<Q, R>` | application | Handler interface for queries |
| `Mediator` | application | Dispatches commands and queries to handlers |
| `TransactionRunner` | application | Runs code within a transaction boundary |
| `DomainEventPublisher` | application | Publishes domain events |
| `ApiResponse<T>` | interfaces | Standard API response wrapper |

**Critical rule**: The shared kernel must remain minimal. Only genuinely cross-cutting abstractions belong here. Business logic or module-specific types must never be placed in shared.

---

## Application Bootstrap

The `app/` module is the Spring Boot application entry point. It:

- Scans all module packages.
- Configures shared infrastructure (security, database, etc.).
- Defines no business logic.

```java
@SpringBootApplication(scanBasePackages = "com.cyna")
public class CynaApplication {
    public static void main(String[] args) {
        SpringApplication.run(CynaApplication.class, args);
    }
}
```

---

## Configuration Management

Application configuration uses Spring profiles:

| Profile | Purpose |
|---------|---------|
| `local` | Local development with Docker Compose |
| `test` | Automated testing (Testcontainers) |
| `staging` | Pre-production environment |
| `production` | Production environment |

Sensitive values (secrets, API keys) are injected via environment variables, never committed to the repository.

---

## Related Documents

- [Architecture Overview](architecture-overview.md)
- [Module Structure](module-structure.md)
- [Dependency Rules](dependency-rules.md)
- [Patterns](../development/patterns.md)
