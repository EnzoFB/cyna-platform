# Dependency Rules

## Purpose

This document defines the strict dependency rules governing the CYNA Platform backend. These rules ensure Clean Architecture integrity, module isolation, and microservice-readiness. All rules are enforced by **ArchUnit tests** in CI.

---

## Fundamental Principle

> Dependencies always point inward. Outer layers depend on inner layers. Inner layers never depend on outer layers.

```
interfaces → application → domain ← infrastructure
                ↑                         |
                └─────────────────────────┘
```

Infrastructure depends on domain (to implement repository interfaces) and on application (to wire handlers). It does **not** flow inward — domain and application never import infrastructure.

---

## Layer Dependency Matrix

### Within a Single Module

| Source Layer | Can Depend On | Cannot Depend On |
|-------------|--------------|-----------------|
| **domain** | shared/domain, Java stdlib | application, interfaces, infrastructure, Spring, JPA, Jakarta |
| **application** | domain, shared/domain, shared/application | interfaces, infrastructure, Spring (except via DI interfaces) |
| **interfaces** | application, shared | infrastructure (directly) |
| **infrastructure** | domain, application, shared, Spring, JPA | interfaces |

### Detailed Rules

#### Domain Layer

**Allowed imports:**

```
java.*
java.util.*
java.time.*
com.cyna.shared.domain.*
com.cyna.{this-module}.domain.*
```

**Forbidden imports:**

```
org.springframework.*
jakarta.*
javax.*
org.hibernate.*
com.cyna.{this-module}.application.*
com.cyna.{this-module}.interfaces.*
com.cyna.{this-module}.infrastructure.*
com.cyna.{other-module}.*            # No cross-module domain access
```

#### Application Layer

**Allowed imports:**

```
java.*
com.cyna.shared.domain.*
com.cyna.shared.application.*
com.cyna.{this-module}.domain.*
com.cyna.{this-module}.application.*
com.cyna.{other-module}.application.api.*   # Only public API interfaces
```

**Forbidden imports:**

```
org.springframework.*                # Exception: @Service, @Component for DI
jakarta.*
com.cyna.{this-module}.interfaces.*
com.cyna.{this-module}.infrastructure.*
com.cyna.{other-module}.domain.*     # No cross-module domain access
com.cyna.{other-module}.interfaces.*
com.cyna.{other-module}.infrastructure.*
```

#### Interfaces Layer

**Allowed imports:**

```
java.*
org.springframework.web.*
org.springframework.http.*
jakarta.validation.*
com.cyna.shared.*
com.cyna.{this-module}.application.*
com.cyna.{this-module}.interfaces.*
```

**Forbidden imports:**

```
com.cyna.{this-module}.domain.*      # Interfaces should not know domain directly
com.cyna.{this-module}.infrastructure.*
com.cyna.{other-module}.*
org.hibernate.*
```

> **Note**: The interfaces layer interacts with the application layer through commands, queries, and read models — never through domain objects. This prevents domain model leakage into the API surface.

#### Infrastructure Layer

**Allowed imports:**

```
java.*
org.springframework.*
jakarta.persistence.*
org.hibernate.*
com.cyna.shared.*
com.cyna.{this-module}.domain.*
com.cyna.{this-module}.application.*
com.cyna.{this-module}.infrastructure.*
```

**Forbidden imports:**

```
com.cyna.{this-module}.interfaces.*
com.cyna.{other-module}.*           # No cross-module access at infrastructure level
```

---

## Cross-Module Dependency Rules

### Allowed

```
Module A (application) → Module B (application.api.*)
```

A module may depend on another module's **public API interface** — a well-defined contract in the application layer.

### Allowed

```
Module A (application.eventhandler) → consumes DomainEvent from Module B
```

A module may handle domain events published by another module.

### Forbidden

```
Module A → Module B (domain/infrastructure/interfaces)
```

No module may directly access another module's domain, infrastructure, or interfaces layer.

### Forbidden

```
Module A (infrastructure) → Module B (infrastructure)
```

No cross-module database access. Each module owns its data exclusively.

---

## Visual Summary

```
┌──────────────────────────────────────────────────────────────────┐
│                        Module A                                  │
│                                                                  │
│  ┌──────────┐    ┌──────────────┐     ┌─────────────────────┐    │
│  │interfaces│───▶│ application  │───▶│      domain         │    │
│  └──────────┘    └──────┬───────┘     └──────────▲──────────┘    │
│                         │                        │               │
│                         │                        │               │
│                  ┌──────▼───────────────────────┐│               │
│                  │     infrastructure           ││               │
│                  └──────────────────────────────┘│               │
│                                                  │               │
│         ╔═══════════════╗                        │               │
│         ║  Public API   ║ ◄───── exposed to ──── │               │
│         ╚═══════╤═══════╝       other modules    │               │
│                 │                                │               │
└─────────────────┼────────────────────────────────┘               │
                  │                                                │
                  │  (only via Public API interface)               │
                  ▼                                                │
┌────────────────────────────────────────────────────────────────┐ │
│                        Module B                                │ │
│                                                                │ │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────────┐   │ │
│  │interfaces │───▶│ application  │───▶│      domain       |   │ │
│  └──────────┘    └──────────────┘    └─────────────────────┘   │ │
│                                                                │ │
└────────────────────────────────────────────────────────────────┘ │
```

---

## Framework Usage Rules

| Framework / Library | Allowed In | Forbidden In |
|--------------------|-----------|-------------|
| Spring Framework (`@Service`, `@Component`, `@Autowired`) | infrastructure, interfaces | domain |
| Spring Web (`@RestController`, `@RequestMapping`) | interfaces | domain, application, infrastructure |
| Spring Data JPA | infrastructure | domain, application, interfaces |
| JPA / Jakarta Persistence | infrastructure | domain, application, interfaces |
| Jakarta Validation (`@Valid`, `@NotNull`) | interfaces (request DTOs only) | domain, application, infrastructure |
| Hibernate | infrastructure | domain, application, interfaces |
| Lombok | all layers (limited) | domain (discouraged) |
| Java stdlib (`java.*`) | all layers | — |

---

## ArchUnit Enforcement

All dependency rules are encoded as ArchUnit tests and run in CI. A violation fails the build.

```java
@AnalyzeClasses(packages = "com.cyna")
class DependencyRulesTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_jpa =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "javax.persistence..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_jakarta_validation =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("jakarta.validation..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_interfaces =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..interfaces..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_interfaces =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..interfaces..");

    @ArchTest
    static final ArchRule interfaces_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..interfaces..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");
}
```

See [Architecture Tests](../testing/architecture-tests.md) for the full ArchUnit test suite.

---

## Common Violations and How to Fix Them

| Violation | Symptom | Fix |
|-----------|---------|-----|
| Domain imports `@Entity` | JPA annotation in domain | Create separate JPA entity in infrastructure |
| Domain imports `@NotNull` | Jakarta Validation in domain | Use Guard clauses in domain constructors |
| Controller accesses repository | Bypasses application layer | Route through Mediator → Handler → Repository |
| Handler imports JPA class | Application depends on infrastructure | Inject domain repository interface; implement in infrastructure |
| Module A reads Module B's DB table | Cross-module DB access | Use Module B's public API instead |
| Module A imports Module B's domain | Cross-module domain coupling | Depend only on Module B's `application.api` |

---

## Related Documents

- [Architecture Overview](architecture-overview.md)
- [Module Structure](module-structure.md)
- [Architecture Tests](../testing/architecture-tests.md)
- [Inter-Module Communication](inter-module-communication.md)
