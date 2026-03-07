# Architecture Overview

## Purpose

This document describes the high-level architecture of the CYNA Platform, the guiding principles behind major design decisions, and the constraints the development team must respect.

---

## System Context

CYNA Platform is a B2B SaaS product enabling companies to purchase and manage cybersecurity services — specifically SOC (Security Operations Center), EDR (Endpoint Detection and Response), and XDR (Extended Detection and Response) subscriptions — through a self-service online platform.

### Actors

| Actor | Role | Interface |
|-------|------|-----------|
| Customer | Browses, purchases, and manages cybersecurity services | Angular PWA |
| Administrator | Manages products, orders, users, and platform configuration | Angular Backoffice |
| External Systems | Payment gateways, notification services, identity providers | Backend API |

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENTS                                │
│  ┌──────────────┐              ┌───────────────────┐        │
│  │  Angular PWA │              │ Angular Backoffice│        │
│  │  (Customer)  │              │ (Admin)           │        │
│  └──────┬───────┘              └────────┬──────────┘        │
│         │                               │                   │
│         └──────────┬────────────────────┘                   │
│                    │ HTTPS / REST                           │
└────────────────────┼────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                   BACKEND API                               │
│              (Java 21 / Spring Boot 3.x)                    │
│                                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │  Module  │ │  Module  │ │  Module  │ │  Module  │        │
│  │  User    │ │  Product │ │  Order   │ │  Payment │        │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘        │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Shared Kernel / Commons                 │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
                ┌─────────▼─────────┐
                │    PostgreSQL     │
                │   (per-module     │
                │    schemas)       │
                └───────────────────┘
```

---

## Architectural Style

### Modular Monolith

The backend is a **Modular Monolith**: a single deployable unit composed of well-isolated, loosely coupled modules. Each module encapsulates a bounded context and follows the same internal layering.

**Why Modular Monolith?**

- **Simplicity**: A single deployment artifact simplifies operations during the initial growth phase.
- **Isolation**: Module boundaries enforce encapsulation, preventing the "big ball of mud" that plagues traditional monoliths.
- **Microservice-readiness**: Each module is designed to be extractable into an independent microservice with minimal refactoring, because modules communicate only through well-defined public APIs and domain events — never through shared database tables.

### Clean Architecture

Every module follows **Clean Architecture**. The codebase is organized in concentric layers with strict dependency rules:

```
┌────────────────────────────────────────────┐
│              interfaces                    │  ← Controllers, DTOs
│  ┌──────────────────────────────────────┐  │
│  │          application                 │  │  ← Use cases, commands, queries
│  │  ┌──────────────────────────────┐    │  │
│  │  │          domain              │    │  │  ← Entities, value objects, events
│  │  └──────────────────────────────┘    │  │
│  └──────────────────────────────────────┘  │
│              infrastructure                │  ← JPA adapters, external services
└────────────────────────────────────────────┘
```

**Dependency rule**: Dependencies always point inward. The domain layer has zero external dependencies.

### Domain-Driven Design (DDD)

The system uses DDD at both strategic and tactical levels:

- **Strategic**: The business domain is decomposed into bounded contexts, each mapped to a module.
- **Tactical**: Within each module, the domain is modeled using aggregates, entities, value objects, domain events, and repository interfaces.

---

## Core Architectural Principles

### 1. Domain Purity

The domain layer is **pure Java**. It must not depend on:

- Spring Framework
- JPA / Hibernate
- Jakarta Validation (`@NotNull`, `@Size`, etc.)
- Any framework or library

The domain expresses business rules through code — constructors, factory methods, guard clauses, and invariant enforcement.

### 2. Dependency Inversion

All infrastructure concerns (database, messaging, external APIs) are accessed through **interfaces defined in the domain or application layer**, implemented in the infrastructure layer.

### 3. Module Isolation

- Each module owns its data. **Cross-module database access is strictly forbidden.**
- Modules communicate only via:
  - **Application-layer public APIs** (synchronous calls via interfaces)
  - **Domain events** (asynchronous, loosely coupled reactions)
- Direct class references between module internals are forbidden.

### 4. CQRS

Commands (write operations) and queries (read operations) are handled by separate objects, dispatched through a Mediator. This separation:

- Keeps each handler focused on a single responsibility.
- Enables independent scaling of reads and writes if the system moves to microservices.
- Improves testability.

### 5. Explicit Error Handling

Business errors are communicated via the **Result Pattern**, not exceptions. Exceptions are reserved for truly unexpected infrastructure failures.

### 6. Microservice-Readiness

Every design decision is made with the assumption that any module may become a standalone service. This means:

- No shared mutable state between modules.
- No cross-module database joins.
- Communication contracts are explicit and versioned.
- Each module could have its own database schema today.

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.x (latest LTS) |
| Build Tool | Gradle (Kotlin DSL) | Latest |
| Database | PostgreSQL | 16+ |
| Migrations | Flyway | Latest |
| Frontend (Customer) | Angular | 20 |
| Frontend (Admin) | Angular | 20 |
| Authentication | JWT (access + refresh tokens) | — |
| Authorization | RBAC | — |
| Testing | JUnit 5, Mockito, ArchUnit, Testcontainers | Latest |

---

## Quality Attributes

| Attribute | Strategy |
|-----------|----------|
| **Maintainability** | Clean Architecture, strict layering, coding standards, architecture tests |
| **Testability** | Domain purity, dependency injection, Result Pattern, in-memory test doubles |
| **Scalability** | CQRS, module isolation, microservice-ready design |
| **Security** | JWT + refresh token rotation, RBAC, input validation at the interfaces layer |
| **Reliability** | Transactional consistency via TransactionRunner, domain invariants, idempotency |
| **Evolvability** | Module isolation, event-driven integration, anti-corruption layers for externals |

---

## Architectural Constraints

| Constraint | Rationale |
|-----------|-----------|
| Domain must be framework-free | Ensures business logic is portable and testable without infrastructure |
| No cross-module DB access | Preserves module autonomy; enables future extraction to microservices |
| All module communication via public APIs or events | Enforces loose coupling |
| CQRS separation is mandatory | Ensures clear separation of read/write paths |
| Result Pattern for business errors | Makes error paths explicit and composable; avoids exception-driven control flow |
| PostgreSQL per-module schemas | Physical isolation of module data within a single database instance |
| Flyway for all schema changes | Ensures reproducible, version-controlled database evolution |
| ArchUnit tests for dependency rules | Automates enforcement of architectural constraints in CI |

---

## Related Documents

- [Backend Architecture](backend-architecture.md) — Detailed backend structure
- [Frontend Architecture](frontend-architecture.md) — Angular application design
- [Module Structure](module-structure.md) — Internal module layout
- [Dependency Rules](dependency-rules.md) — Allowed and forbidden dependencies
- [Inter-Module Communication](inter-module-communication.md) — Module integration patterns
