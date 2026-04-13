# Code Review Guidelines

## Purpose

This document defines the code review process, expectations, and checklist for the CYNA Platform team. Code review is a critical quality gate — every change to `main` must be reviewed.

---

## Goals of Code Review

1. **Correctness** — Does the code do what it's supposed to?
2. **Architecture compliance** — Does it follow Clean Architecture, DDD, and project patterns?
3. **Quality** — Is the code readable, maintainable, and well-tested?
4. **Knowledge sharing** — Does the team learn from each other?
5. **Security** — Are there any security vulnerabilities?

---

## Process

### Who Reviews?

| Change Type | Required Reviewers |
|-------------|-------------------|
| Standard feature/fix | 1 team member |
| Architecture change | 2 team members (including senior/architect) |
| Security-related change | 2 team members (including security-aware reviewer) |
| Database migration | 2 team members |
| Shared kernel change | 2 team members |

### Turnaround Time

| Priority | Expectation |
|----------|------------|
| Standard | Review within 4 business hours |
| Urgent/hotfix | Review within 1 hour |
| Critical security | Review immediately |

### Author Responsibilities

| Before requesting review... |
|----------------------------|
| Self-review your own PR first |
| Ensure CI passes (all tests, lint, build) |
| Write a clear PR description |
| Keep the PR small (max 400 lines, ideally < 200) |
| Add context comments on non-obvious code |
| Link the ticket |

### Reviewer Responsibilities

| During review... |
|-----------------|
| Review within the expected turnaround time |
| Be constructive, specific, and kind |
| Explain the *why* behind suggestions |
| Distinguish between blocking issues and suggestions |
| Approve only when all blocking issues are resolved |
| Test mentally — trace the code path for key scenarios |

---

## Review Checklist

### Architecture

- [ ] Code is in the correct layer (domain, application, interfaces, infrastructure)?
- [ ] Dependencies point inward (no domain → infrastructure)?
- [ ] Module isolation is respected (no cross-module domain/infra access)?
- [ ] JPA entities are separate from domain entities?
- [ ] Domain layer is free of Spring/JPA/Jakarta annotations?
- [ ] Controllers delegate to Mediator only (no business logic)?
- [ ] CQRS is followed (commands and queries are separate)?
- [ ] Result Pattern is used for business errors (no exceptions)?

### Domain

- [ ] Aggregate root enforces all invariants?
- [ ] Factory methods are used (no public constructors)?
- [ ] Value objects are immutable?
- [ ] Domain events are raised for significant state changes?
- [ ] Events are immutable records with primitives only?
- [ ] Collections are exposed as unmodifiable?

### Application

- [ ] One handler per command/query?
- [ ] Handler coordinates domain objects (no business logic in handler)?
- [ ] TransactionRunner is used (no `@Transactional`)?
- [ ] Public API interface is used for cross-module access?
- [ ] Read models are returned from query handlers (not entities)?

### API / Controllers

- [ ] REST conventions followed (proper HTTP methods, status codes)?
- [ ] Request DTOs have Jakarta Validation annotations?
- [ ] Response DTOs don't expose domain entities?
- [ ] Standard `ApiResponse<T>` wrapper is used?
- [ ] Error cases return proper error codes and messages?

### Database

- [ ] Migration follows naming convention (`V{n}__{description}.sql`)?
- [ ] Migration is backwards-compatible?
- [ ] Table/column naming follows conventions?
- [ ] Indexes are appropriate?
- [ ] No cross-schema foreign keys?
- [ ] Constraints are named explicitly?

### Security

- [ ] Sensitive data is not logged?
- [ ] Endpoints have proper authorization (`@PreAuthorize` or security config)?
- [ ] User input is validated?
- [ ] No SQL injection risk?
- [ ] Sort and filter parameters use allow-lists (see `security/sql-injection.md`)?
- [ ] Text fields reject HTML (`@NoHtml`, see `security/xss.md`)?
- [ ] HTTPS and security headers configured (see `security/https-headers.md`)?
- [ ] CSRF configured when cookie-based auth is used (see `security/csrf.md`)?
- [ ] No secrets in code or commits?
- [ ] Resource ownership is checked where applicable?

### Testing

- [ ] Unit tests cover business rules and edge cases?
- [ ] Both success and failure paths are tested?
- [ ] Tests follow AAA pattern (Arrange, Act, Assert)?
- [ ] Test names are descriptive (`should_..._when_...`)?
- [ ] No test depends on external state or execution order?
- [ ] Integration tests use Testcontainers (not H2)?

### Code Quality

- [ ] Naming follows conventions?
- [ ] Methods are short and focused?
- [ ] No code duplication?
- [ ] No dead code or commented-out code?
- [ ] No TODOs without ticket references?
- [ ] Appropriate use of `final`, `var`, records?
- [ ] No unnecessary complexity?

---

## Comment Conventions

Use prefixes to indicate the nature of your review comment:

| Prefix | Meaning | Blocks Merge? |
|--------|---------|---------------|
| `BLOCKER:` | Must be fixed before merge | Yes |
| `ISSUE:` | Should be fixed, but discuss if context is missing | Yes |
| `SUGGESTION:` | Nice to have, not required | No |
| `QUESTION:` | Need clarification to continue review | No (informational) |
| `NIT:` | Minor style/formatting preference | No |
| `PRAISE:` | Something done well! | No |

### Examples

```
BLOCKER: This handler modifies state in a query handler — this violates CQRS.
Commands modify state; queries only read.

ISSUE: The domain entity has a @Column annotation.
JPA annotations should only be on JpaEntity classes in the infrastructure layer.

SUGGESTION: Consider extracting this validation into a value object.
It would make the aggregate cleaner and the validation reusable.

QUESTION: Is this intentionally using a different error code than the standard?

NIT: This could be a record instead of a class — it's immutable and has no behavior.

PRAISE: Great use of the Result pattern here — very clean error handling!
```

---

## When to Request Changes vs Approve

### Request Changes

- Architecture violations (layer dependencies, module coupling)
- Missing or broken tests
- Security vulnerabilities
- Business logic errors
- Missing error handling

### Approve (with comments)

- Minor style suggestions
- Optional refactoring ideas
- Knowledge-sharing comments
- Questions that don't block functionality

### Approve (clean)

- Code is correct, well-tested, follows all conventions
- No concerns

---

## Pair Review for Critical Changes

For the following types of changes, consider a **live pair review** (screen share):

- New module creation
- Architecture changes
- Complex domain logic
- Security-sensitive code
- Performance-critical paths

---

## Anti-Patterns in Code Review

| Anti-Pattern | Why It's Harmful | Instead... |
|-------------|-----------------|-----------|
| "Looks good to me" without reading | Rubber-stamp reviews miss bugs | Take time to read properly |
| Nitpicking style only | Misses real issues | Focus on correctness and architecture first |
| Rewriting the author's code | Discourages contribution | Suggest alternatives, explain why |
| Personal preference as blocking | Creates friction | Mark as NIT or SUGGESTION |
| "Let's just merge and fix later" | "Later" never comes | Fix before merge |
| Review after 3 days | Blocks the author, stales the PR | Review within 4 hours |
| 1000-line PR | Impossible to review thoroughly | Split into smaller PRs |

---

## Metrics

Track these to improve the review process:

| Metric | Target |
|--------|--------|
| PR review turnaround | < 4 hours |
| Average PR size | < 200 lines changed |
| PR merge time | < 1 business day |
| Review comments per PR | 2–10 (meaningful) |
| Architecture violations caught | Decreasing trend |

---

## Related Documents

- [Git Workflow](git-workflow.md)
- [Coding Standards](../development/coding-standards.md)
- [Architecture Tests](../testing/architecture-tests.md)
- [Dependency Rules](../architecture/dependency-rules.md)
