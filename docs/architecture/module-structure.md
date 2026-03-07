# Module Structure

## Purpose

This document defines the internal structure every backend module must follow. Consistency across modules is critical for maintainability, onboarding, and future microservice extraction.

---

## Canonical Module Layout

Every module follows this exact package and folder structure:

```
modules/{module-name}/
├── domain/
│   ├── model/
│   │   ├── {AggregateRoot}.java
│   │   ├── {Entity}.java
│   │   ├── {ValueObject}.java
│   │   └── {Enum}.java
│   ├── event/
│   │   ├── {DomainEvent}.java
│   │   └── ...
│   ├── repository/
│   │   └── {AggregateRoot}Repository.java
│   ├── service/
│   │   └── {DomainService}.java         # (optional)
│   └── exception/
│       └── {DomainException}.java       # (optional, only for truly exceptional cases)
│
├── application/
│   ├── command/
│   │   ├── create/
│   │   │   ├── Create{Entity}Command.java
│   │   │   └── Create{Entity}CommandHandler.java
│   │   ├── update/
│   │   │   ├── Update{Entity}Command.java
│   │   │   └── Update{Entity}CommandHandler.java
│   │   └── delete/
│   │       ├── Delete{Entity}Command.java
│   │       └── Delete{Entity}CommandHandler.java
│   ├── query/
│   │   ├── getbyid/
│   │   │   ├── Get{Entity}ByIdQuery.java
│   │   │   ├── Get{Entity}ByIdQueryHandler.java
│   │   │   └── {Entity}ReadModel.java
│   │   └── list/
│   │       ├── List{Entities}Query.java
│   │       ├── List{Entities}QueryHandler.java
│   │       └── {Entity}SummaryReadModel.java
│   ├── port/
│   │   └── {ExternalService}Port.java   # (optional)
│   ├── api/
│   │   └── {Module}QueryApi.java        # Public API for other modules
│   └── eventhandler/
│       └── On{DomainEvent}Handler.java  # Handles events from other modules
│
├── interfaces/
│   ├── rest/
│   │   ├── {Entity}Controller.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── Create{Entity}Request.java
│   │   │   │   └── Update{Entity}Request.java
│   │   │   └── response/
│   │   │       ├── {Entity}Response.java
│   │   │       └── {Entity}ListResponse.java
│   │   └── mapper/
│   │       └── {Entity}DtoMapper.java
│   └── eventlistener/
│       └── {DomainEvent}Listener.java   # Spring event listener wiring
│
└── infrastructure/
    ├── persistence/
    │   ├── entity/
    │   │   └── {Entity}JpaEntity.java
    │   ├── repository/
    │   │   ├── Jpa{Entity}RepositoryAdapter.java
    │   │   └── SpringData{Entity}Repository.java
    │   └── mapper/
    │       └── {Entity}JpaMapper.java
    ├── adapter/
    │   └── {ExternalService}Adapter.java  # (optional)
    └── config/
        └── {Module}Config.java
```

---

## Package Naming Convention

Base package: `com.cyna.{module}`

| Layer | Package |
|-------|---------|
| Domain | `com.cyna.{module}.domain` |
| Application | `com.cyna.{module}.application` |
| Interfaces | `com.cyna.{module}.interfaces` |
| Infrastructure | `com.cyna.{module}.infrastructure` |

### Examples

```
com.cyna.order.domain.model.Order
com.cyna.order.domain.event.OrderCreated
com.cyna.order.domain.repository.OrderRepository
com.cyna.order.application.command.create.CreateOrderCommand
com.cyna.order.application.command.create.CreateOrderCommandHandler
com.cyna.order.application.query.getbyid.GetOrderByIdQuery
com.cyna.order.interfaces.rest.OrderController
com.cyna.order.interfaces.rest.dto.request.CreateOrderRequest
com.cyna.order.infrastructure.persistence.entity.OrderJpaEntity
com.cyna.order.infrastructure.persistence.repository.JpaOrderRepositoryAdapter
```

---

## Layer Responsibilities

### Domain Layer

| Component | Naming | Responsibility |
|-----------|--------|---------------|
| Aggregate Root | `Order`, `User`, `Product` | Root entity governing a consistency boundary |
| Entity | `OrderLine`, `Address` | Entity within an aggregate |
| Value Object | `Money`, `Email`, `OrderId` | Immutable, identity-less value |
| Domain Event | `OrderCreated`, `UserRegistered` | Record of a domain-significant occurrence |
| Repository Interface | `OrderRepository` | Port for aggregate persistence |
| Domain Service | `PricingService` | Stateless business logic spanning aggregates |

### Application Layer

| Component | Naming | Responsibility |
|-----------|--------|---------------|
| Command | `CreateOrderCommand` | DTO representing an intent to change state |
| Command Handler | `CreateOrderCommandHandler` | Executes a command using domain objects |
| Query | `GetOrderByIdQuery` | DTO representing an intent to read data |
| Query Handler | `GetOrderByIdQueryHandler` | Reads data and returns a read model |
| Read Model | `OrderReadModel` | Flat DTO optimized for the consumer |
| Port Interface | `PaymentGatewayPort` | Interface for external service integration |
| Public API | `ProductQueryApi` | Interface exposed to other modules |
| Event Handler | `OnOrderPaidHandler` | Reacts to domain events from other modules |

### Interfaces Layer

| Component | Naming | Responsibility |
|-----------|--------|---------------|
| Controller | `OrderController` | Maps HTTP requests to commands/queries via Mediator |
| Request DTO | `CreateOrderRequest` | Validates and carries HTTP input |
| Response DTO | `OrderResponse` | Serialized HTTP output |
| DTO Mapper | `OrderDtoMapper` | Converts between request/response DTOs and commands/queries |
| Event Listener | `OrderCreatedListener` | Bridges Spring events to application event handlers |

### Infrastructure Layer

| Component | Naming | Responsibility |
|-----------|--------|---------------|
| JPA Entity | `OrderJpaEntity` | ORM-mapped database row |
| Repository Adapter | `JpaOrderRepositoryAdapter` | Implements domain repository using JPA |
| Spring Data Repo | `SpringDataOrderRepository` | Spring Data JPA interface |
| JPA Mapper | `OrderJpaMapper` | Converts between domain objects and JPA entities |
| External Adapter | `StripePaymentAdapter` | Implements port interfaces for external systems |
| Config | `OrderModuleConfig` | Spring configuration for the module |

---

## Mandatory Rules

### 1. Every module must have all four layers

Even if a layer is thin (e.g., a lookup module with minimal domain logic), the layers must exist for consistency.

### 2. No layer shortcuts

- Controllers must not directly access repositories.
- Controllers must not contain business logic.
- Handlers must not contain SQL or JPA code.
- Domain must not import infrastructure classes.

### 3. JPA entities are NOT domain entities

The infrastructure layer maintains its own JPA entity classes with JPA annotations. The domain layer has pure Java classes. Mappers convert between the two.

**Why?** This decouples the domain model from the database schema, allowing both to evolve independently.

### 4. One aggregate root per module (primary)

Each module has a primary aggregate root. Additional aggregates are allowed but should be rare and justified.

### 5. Command and Query subpackages

Each command or query has its own subpackage containing all related classes:

```
application/
  command/
    create/
      CreateOrderCommand.java
      CreateOrderCommandHandler.java
```

This prevents files from being scattered and makes each use case a self-contained unit.

---

## Module Checklist

When creating a new module, verify:

- [ ] Domain layer contains at least one aggregate root
- [ ] Aggregate root enforces its invariants in constructors/methods
- [ ] Repository interface is defined in domain
- [ ] Repository implementation is in infrastructure
- [ ] JPA entity is separate from domain entity
- [ ] JPA mapper converts between domain and JPA
- [ ] Commands and queries have dedicated handlers
- [ ] Handlers use Result Pattern for error handling
- [ ] Controller delegates to Mediator only
- [ ] Request DTOs use Jakarta Validation
- [ ] Module has its own database schema
- [ ] Flyway migration exists for the schema
- [ ] ArchUnit tests cover dependency rules
- [ ] Public API interface exists if other modules need data

---

## Related Documents

- [Backend Architecture](backend-architecture.md)
- [Dependency Rules](dependency-rules.md)
- [Coding Standards](../development/coding-standards.md)
