# Coding Standards

## Purpose

This document defines the coding standards, naming conventions, and formatting rules for the CYNA Platform. Consistency across the codebase is non-negotiable — it reduces cognitive load, simplifies code reviews, and accelerates onboarding.

---

## General Principles

1. **Readability over cleverness.** Code is read far more often than it is written.
2. **Explicit over implicit.** Make intentions clear. Avoid magic.
3. **Small over large.** Small classes, small methods, small commits.
4. **Immutability by default.** Make things mutable only when necessary.
5. **Fail fast.** Validate inputs early. Guard clauses before logic.

---

## Java Coding Standards

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Package | lowercase, dot-separated | `com.cyna.order.domain.model` |
| Class | PascalCase, noun | `OrderRepository`, `CreateOrderCommand` |
| Interface | PascalCase, noun (no `I` prefix) | `OrderRepository`, not `IOrderRepository` |
| Method | camelCase, verb | `createOrder()`, `findById()` |
| Variable | camelCase, descriptive | `orderTotal`, `customerId` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| Enum | PascalCase class, UPPER_SNAKE_CASE values | `OrderStatus.PENDING` |
| Generic type | Single uppercase letter or short name | `T`, `ID`, `R` |
| Test class | `{ClassUnderTest}Test` | `OrderTest`, `CreateOrderCommandHandlerTest` |
| Test method | `should_{expected}_{condition}` | `should_fail_when_order_is_cancelled()` |

### Class Naming by Layer

| Layer | Component | Pattern | Example |
|-------|-----------|---------|---------|
| domain | Aggregate | `{Name}` | `Order` |
| domain | Value Object | `{Name}` | `Money`, `Email`, `OrderId` |
| domain | Domain Event | `{Name}{PastVerb}` | `OrderCreated`, `UserRegistered` |
| domain | Repository Interface | `{Aggregate}Repository` | `OrderRepository` |
| domain | Domain Service | `{Name}Service` | `PricingService` |
| application | Command | `{Verb}{Entity}Command` | `CreateOrderCommand` |
| application | Command Handler | `{Command}Handler` | `CreateOrderCommandHandler` |
| application | Query | `{Verb}{Entity}Query` | `GetOrderByIdQuery` |
| application | Query Handler | `{Query}Handler` | `GetOrderByIdQueryHandler` |
| application | Read Model | `{Entity}ReadModel` | `OrderReadModel` |
| application | Port | `{Service}Port` | `PaymentGatewayPort` |
| application | Public API | `{Module}QueryApi` | `ProductQueryApi` |
| interfaces | Controller | `{Entity}Controller` | `OrderController` |
| interfaces | Request DTO | `{Action}{Entity}Request` | `CreateOrderRequest` |
| interfaces | Response DTO | `{Entity}Response` | `OrderResponse` |
| infrastructure | JPA Entity | `{Entity}JpaEntity` | `OrderJpaEntity` |
| infrastructure | Adapter | `Jpa{Entity}RepositoryAdapter` | `JpaOrderRepositoryAdapter` |
| infrastructure | Mapper | `{Entity}JpaMapper` | `OrderJpaMapper` |
| infrastructure | Config | `{Module}Config` | `OrderModuleConfig` |

### Formatting Rules

- **Indentation**: 4 spaces (no tabs).
- **Line length**: 120 characters maximum.
- **Braces**: K&R style (opening brace on same line).
- **Blank lines**: One blank line between methods. Two blank lines between class-level sections.
- **Imports**: No wildcard imports (`import java.util.*`). Organize imports: Java → Jakarta → third-party → project.

### Java-Specific Rules

| Rule | Example |
|------|---------|
| Use `final` for fields and parameters where possible | `private final OrderRepository repo;` |
| Use records for immutable data carriers | `public record OrderId(UUID value) {}` |
| Use `Optional` for return types, never as method parameters | `Optional<Order> findById(OrderId id)` |
| Prefer `List.of()`, `Map.of()` for immutable collections | `List.of("a", "b")` |
| Use `var` only when the type is obvious from the right side | `var order = Order.create(...)` |
| Use `sealed` interfaces for closed type hierarchies | `sealed interface Result<T> permits Success, Failure` |
| Avoid `null` — use `Optional`, empty collections, or `Result` | — |
| No checked exceptions for business logic | Return `Result<T>` instead |

### Method Design

- **Maximum 20 lines per method** (guideline, not hard rule).
- **Maximum 3 parameters** — if more, use a command/query object.
- **Single responsibility** — a method does one thing.
- **Guard clause first**, then happy path:

```java
public Result<OrderId> handle(CreateOrderCommand command) {
    // Guard clauses first
    if (command.items().isEmpty()) {
        return Result.failure("Order must have at least one item");
    }

    var customer = customerQueryApi.getById(command.customerId());
    if (customer.isEmpty()) {
        return Result.failure("Customer not found");
    }

    // Happy path
    Order order = Order.create(command.customerId(), buildLines(command.items()));
    orderRepository.save(order);
    return Result.success(order.getId());
}
```

---

## TypeScript Coding Standards (Frontend)

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| File | kebab-case | `product-list.component.ts` |
| Class | PascalCase | `ProductListComponent` |
| Interface | PascalCase (no `I` prefix) | `Product`, not `IProduct` |
| Method | camelCase | `getProducts()` |
| Variable | camelCase | `productList` |
| Constant | camelCase or UPPER_SNAKE_CASE | `defaultPageSize` or `DEFAULT_PAGE_SIZE` |
| Enum | PascalCase class, PascalCase values | `OrderStatus.Pending` |
| Service | `{Name}Service` | `CatalogService` |
| Component | `{Name}Component` | `ProductListComponent` |
| Guard | `{Name}Guard` (function) | `authGuard` |
| Pipe | `{Name}Pipe` | `CurrencyFormatPipe` |

### Angular File Naming

| Type | Pattern | Example |
|------|---------|---------|
| Component | `{name}.component.ts` | `product-list.component.ts` |
| Service | `{name}.service.ts` | `catalog.service.ts` |
| Model | `{name}.model.ts` | `product.model.ts` |
| Guard | `{name}.guard.ts` | `auth.guard.ts` |
| Interceptor | `{name}.interceptor.ts` | `auth.interceptor.ts` |
| Pipe | `{name}.pipe.ts` | `currency-format.pipe.ts` |
| Directive | `{name}.directive.ts` | `click-outside.directive.ts` |
| Spec | `{name}.spec.ts` | `catalog.service.spec.ts` |

### TypeScript-Specific Rules

| Rule | Rationale |
|------|-----------|
| `strict: true` in tsconfig | Catch errors at compile time |
| No `any` type — use `unknown` if truly unknown | Type safety |
| Use interfaces for object shapes, types for unions | Convention clarity |
| Use `readonly` for properties that should not change | Immutability |
| Use `const` by default, `let` only when reassignment is needed | Immutability |
| Never use `var` | Scoping issues |
| Use template literals for string interpolation | `Hello ${name}` |

---

## Package / Folder Conventions

### Backend

```
com.cyna.{module}.{layer}.{concern}
```

See [Module Structure](../architecture/module-structure.md) for the complete breakdown.

### Frontend

```
features/{feature-name}/
  components/
  services/
  models/
  store/
  {feature-name}.routes.ts
  index.ts
```

---

## Comments and Documentation

### When to Comment

- **Public APIs** — All public methods in application-layer APIs must have Javadoc.
- **Complex algorithms** — Non-obvious logic should have inline comments explaining *why*.
- **Workarounds** — Document *why* a workaround is needed and link to a tracking issue.

### When NOT to Comment

- **Self-explanatory code** — `// increment counter` above `counter++` adds noise.
- **Commented-out code** — Delete it. Git has history.
- **TODOs without tickets** — Every `TODO` must reference a ticket number: `// TODO(CYNA-1234): ...`

### Javadoc

```java
/**
 * Creates a new order for the specified customer.
 *
 * @param customerId the customer placing the order
 * @param lines the items to include in the order
 * @return the created order
 * @throws IllegalArgumentException if customerId is null or lines is empty
 */
public static Order create(CustomerId customerId, List<OrderLine> lines) { ... }
```

---

## Code Smells to Avoid

| Smell | Symptom | Fix |
|-------|---------|-----|
| God class | Class with 500+ lines and many responsibilities | Split into focused classes |
| Long method | Method with 50+ lines | Extract submethods |
| Feature envy | Method uses more data from another class than its own | Move method to the other class |
| Primitive obsession | Passing `String email`, `String name`, `int quantity` | Use value objects: `Email`, `ProductName`, `Quantity` |
| Boolean parameters | `createOrder(true, false)` | Use enums or separate methods |
| Shotgun surgery | One change requires edits in many classes | Consolidate related logic |
| Anemic model | Domain class with only getters/setters | Move logic into domain methods |

---

## Enforcement

| Tool | Purpose |
|------|---------|
| `.editorconfig` | Consistent formatting across IDEs |
| Checkstyle | Java style enforcement |
| ESLint | TypeScript linting |
| Prettier | TypeScript formatting |
| SonarQube | Static analysis, code smells, coverage |
| ArchUnit | Architecture rule enforcement |
| Code review | Human validation of standards compliance |

---

## Related Documents

- [Patterns](patterns.md)
- [Module Structure](../architecture/module-structure.md)
- [Code Review Guidelines](../workflow/code-review-guidelines.md)
