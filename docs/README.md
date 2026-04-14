# CYNA Platform — Technical Documentation

## Overview

**CYNA Platform** is a SaaS platform for selling cybersecurity services (SOC, EDR, XDR) online. The system is designed as a **Modular Monolith** following **Clean Architecture** and **Domain-Driven Design** principles, making it maintainable, testable, and microservice-ready.

This documentation set provides the complete technical reference for the development team. It covers architecture decisions, design patterns, coding standards, security, database management, testing strategies, and team workflows.

---

## System Components

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Customer Frontend | Angular 20 (PWA) | Public-facing storefront for cybersecurity services |
| Backoffice Frontend | Angular 20 | Internal administration dashboard |
| Backend API | Java 21 + Spring Boot 3.x | Business logic, REST API, integrations |
| Database | PostgreSQL | Primary data store |
| Migrations | Flyway | Database schema versioning |

---

## Documentation Map

### Architecture

| Document | Description |
|----------|-------------|
| [Architecture Overview](architecture/architecture-overview.md) | High-level system design, principles, and constraints |
| [Backend Architecture](architecture/backend-architecture.md) | Modular monolith structure, Clean Architecture layers |
| [Frontend Architecture](architecture/frontend-architecture.md) | Angular PWA and Backoffice architecture |
| [Module Structure](architecture/module-structure.md) | Internal structure of each backend module |
| [Dependency Rules](architecture/dependency-rules.md) | Allowed and forbidden dependency directions |
| [Inter-Module Communication](architecture/inter-module-communication.md) | How modules interact without coupling |

### Domain

| Document | Description |
|----------|-------------|
| [Bounded Contexts](domain/bounded-contexts.md) | Strategic DDD decomposition of the business domain |
| [Aggregates](domain/aggregates.md) | Tactical DDD patterns: aggregates, entities, value objects |
| [Domain Events](domain/domain-events.md) | Event definitions, publishing, and handling |

### Development

| Document | Description |
|----------|-------------|
| [Development Setup](development/development-setup.md) | How to set up the local development environment |
| [Coding Standards](development/coding-standards.md) | Naming, formatting, and structural conventions |
| [Patterns](development/patterns.md) | Overview of all design patterns used in the project |
| [Mediator Pattern](development/mediator-pattern.md) | Command/Query dispatching via Mediator |
| [Repository Pattern](development/repository-pattern.md) | Data access abstraction and implementation |
| [Result Pattern](development/result-pattern.md) | Explicit error handling without exceptions |
| [Transaction Management](development/transaction-management.md) | Unit of Work via TransactionRunner |

### API

| Document | Description |
|----------|-------------|
| [API Guidelines](api/api-guidelines.md) | REST conventions, versioning, naming |
| [Error Handling](api/error-handling.md) | Standard error response format |
| [Pagination](api/pagination.md) | Cursor and offset pagination strategies |

### Security

| Document | Description |
|----------|-------------|
| [Authentication](security/authentication.md) | JWT access/refresh token flow |
| [Authorization](security/authorization.md) | RBAC model and enforcement |
| [SQL Injection Protection](security/sql-injection.md) | Rules for query safety and input validation |
| [XSS Protection](security/xss.md) | Rules to prevent cross-site scripting |
| [HTTPS & Security Headers](security/https-headers.md) | HTTPS enforcement and browser security headers |
| [CSRF Protection](security/csrf.md) | CSRF rules and configuration |
| [Security Baseline](security/security-baseline.md) | Consolidated checklist for all security tickets |

### Database

| Document | Description |
|----------|-------------|
| [Database Guidelines](database/database-guidelines.md) | Schema design, naming, indexing |
| [Migrations](database/migrations.md) | Flyway migration conventions |

### Testing

| Document | Description |
|----------|-------------|
| [Testing Strategy](testing/testing-strategy.md) | Unit, integration, and E2E testing |
| [Architecture Tests](testing/architecture-tests.md) | ArchUnit rules to enforce architecture |

### Workflow

| Document | Description |
|----------|-------------|
| [Git Workflow](workflow/git-workflow.md) | Branching model and commit conventions |
| [Code Review Guidelines](workflow/code-review-guidelines.md) | Review checklist and best practices |

### Reference

| Document | Description |
|----------|-------------|
| [Glossary](glossary.md) | Definitions of all technical terms and patterns |
| [Diagrams](diagrams/README.md) | Architecture and flow diagrams |

---

## How to Use This Documentation

1. **New team members** — Start with [Architecture Overview](architecture/architecture-overview.md), then read [Module Structure](architecture/module-structure.md) and [Coding Standards](development/coding-standards.md).
2. **Backend developers** — Read [Backend Architecture](architecture/backend-architecture.md), [Patterns](development/patterns.md), and [Dependency Rules](architecture/dependency-rules.md).
3. **Frontend developers** — Read [Frontend Architecture](architecture/frontend-architecture.md) and [API Guidelines](api/api-guidelines.md).
4. **Reviewers** — Use [Code Review Guidelines](workflow/code-review-guidelines.md) and [Architecture Tests](testing/architecture-tests.md).

---

## Versioning

This documentation is versioned alongside the source code. Any architectural or process change **must** be reflected here before implementation begins.

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0.0 | 2026-03-05 | Architecture Team | Initial documentation set |

