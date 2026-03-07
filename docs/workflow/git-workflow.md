# Git Workflow

## Purpose

This document defines the Git branching model, commit conventions, and collaboration workflow for the CYNA Platform development team.

---

## Branching Model

The project uses a **trunk-based development** approach with short-lived feature branches.

```
main (production-ready)
  │
  ├── feature/CYNA-123-add-order-creation
  ├── feature/CYNA-124-product-catalog-api
  ├── fix/CYNA-125-fix-payment-timeout
  ├── chore/CYNA-126-update-dependencies
  └── release/1.2.0 (when needed)
```

### Branches

| Branch | Purpose | Lifetime | Merged Into |
|--------|---------|----------|-------------|
| `main` | Production-ready code | Permanent | — |
| `feature/{ticket}-{description}` | New features | Short (1–3 days) | `main` |
| `fix/{ticket}-{description}` | Bug fixes | Short (hours–1 day) | `main` |
| `chore/{ticket}-{description}` | Maintenance, refactoring | Short | `main` |
| `release/{version}` | Release stabilization (if needed) | Short | `main` |
| `hotfix/{ticket}-{description}` | Production emergency fix | Very short | `main` |

### Branch Naming

Format: `{type}/{ticket-id}-{short-description}`

| Type | Use Case | Example |
|------|----------|---------|
| `feature` | New functionality | `feature/CYNA-123-add-order-creation` |
| `fix` | Bug fix | `fix/CYNA-125-fix-payment-timeout` |
| `chore` | Maintenance, dependencies | `chore/CYNA-126-update-spring-boot` |
| `refactor` | Code restructuring | `refactor/CYNA-127-extract-payment-module` |
| `docs` | Documentation only | `docs/CYNA-128-add-api-guidelines` |
| `hotfix` | Production emergency | `hotfix/CYNA-130-fix-login-crash` |

### Rules

| Rule | Rationale |
|------|-----------|
| Branch names are lowercase with hyphens | Consistency |
| Always include the ticket ID | Traceability |
| Keep branches short-lived (max 3 days) | Reduce merge conflicts |
| Rebase on `main` before opening PR | Clean history |
| Delete branch after merge | Branch hygiene |
| Never push directly to `main` | All changes go through PRs |

---

## Commit Conventions

The project follows **Conventional Commits** (https://www.conventionalcommits.org/).

### Format

```
{type}({scope}): {short description}

{optional body}

{optional footer}
```

### Types

| Type | Purpose | Example |
|------|---------|---------|
| `feat` | New feature | `feat(order): add order creation endpoint` |
| `fix` | Bug fix | `fix(auth): handle expired refresh token` |
| `refactor` | Code restructuring (no behavior change) | `refactor(product): extract price calculation` |
| `test` | Adding or fixing tests | `test(order): add unit tests for Order.confirm()` |
| `docs` | Documentation | `docs: update API guidelines` |
| `chore` | Maintenance, build, CI | `chore: update Gradle to 8.10` |
| `style` | Formatting (no logic change) | `style: fix indentation in OrderController` |
| `perf` | Performance improvement | `perf(product): add index on product status` |
| `ci` | CI/CD changes | `ci: add integration test stage` |

### Scopes

The scope is the module or area affected:

| Scope | Area |
|-------|------|
| `order` | Order module |
| `product` | Product module |
| `user` | User/IAM module |
| `payment` | Payment module |
| `auth` | Authentication/authorization |
| `api` | API layer (cross-cutting) |
| `db` | Database/migrations |
| `pwa` | Customer frontend |
| `backoffice` | Admin frontend |
| `shared` | Shared kernel |
| `infra` | Infrastructure/DevOps |

### Examples

```bash
# Feature
feat(order): implement order creation command handler

# Bug fix
fix(payment): prevent duplicate payment processing

# Database
feat(db): add V15 migration for order notes column

# Multiple scopes (use comma)
refactor(order,payment): extract Money value object to shared

# Breaking change (add ! after type)
feat(api)!: change pagination response format

BREAKING CHANGE: Pagination response now uses 'items' instead of 'content'
```

### Rules

| Rule | Rationale |
|------|-----------|
| Subject line max 72 characters | Readability in git log |
| Subject in imperative mood ("add", not "added") | Git convention |
| No period at end of subject | Convention |
| Body explains **why**, not **what** | The code shows what; the commit explains why |
| Reference ticket in footer | Traceability |
| One logical change per commit | Atomic, reviewable, revertable |

### Footer Format

```
Refs: CYNA-123
```

or for closing issues:

```
Closes: CYNA-123
```

---

## Pull Request Workflow

### 1. Create Branch

```bash
git checkout main
git pull origin main
git checkout -b feature/CYNA-123-add-order-creation
```

### 2. Develop

- Make small, frequent commits following conventions.
- Push regularly.
- Keep the branch up to date with `main`:

```bash
git fetch origin
git rebase origin/main
```

### 3. Open Pull Request

**PR Title**: Follow commit convention format.

```
feat(order): implement order creation endpoint
```

**PR Description Template**:

```markdown
## Summary

Brief description of what this PR does.

## Changes

- Added `CreateOrderCommand` and `CreateOrderCommandHandler`
- Added `OrderController` with POST endpoint
- Added Flyway migration V10 for orders table
- Added unit tests for Order aggregate

## Type

- [x] Feature
- [ ] Bug fix
- [ ] Refactoring
- [ ] Documentation

## Ticket

Refs: CYNA-123

## Testing

- [x] Unit tests added/updated
- [x] Integration tests added/updated
- [ ] Manual testing completed

## Checklist

- [x] Code follows coding standards
- [x] No new warnings
- [x] Self-reviewed
- [x] Documentation updated if needed
- [x] Migration is backwards-compatible
```

### 4. Code Review

- Minimum **1 approval** required.
- For critical changes (architecture, security, migrations): **2 approvals**.
- All CI checks must pass.
- See [Code Review Guidelines](code-review-guidelines.md).

### 5. Merge

- **Squash merge** for feature branches → clean history on `main`.
- **Merge commit** for release branches → preserves branch history.
- Delete the feature branch after merge.

```bash
# After merge, clean up
git checkout main
git pull origin main
git branch -d feature/CYNA-123-add-order-creation
```

---

## Release Process

### Versioning

The project follows **Semantic Versioning** (semver):

```
MAJOR.MINOR.PATCH

1.0.0 → First release
1.1.0 → New feature (backward-compatible)
1.1.1 → Bug fix
2.0.0 → Breaking change
```

### Release Steps

1. Create release branch: `release/1.2.0`
2. Final testing and bug fixes on release branch.
3. Update version numbers and changelog.
4. Merge to `main`.
5. Tag: `git tag v1.2.0`
6. Deploy.

---

## Hotfix Process

For production emergencies:

1. Branch from `main`: `hotfix/CYNA-999-fix-critical-bug`
2. Fix the issue.
3. Open PR with expedited review.
4. Merge to `main`.
5. Tag with patch version: `v1.2.1`
6. Deploy immediately.

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Pushing directly to `main` | Always use feature branches + PRs |
| Long-lived branches (weeks) | Keep branches under 3 days |
| Vague commit messages ("fix stuff") | Follow Conventional Commits |
| Giant PRs (1000+ lines) | Split into smaller, reviewable PRs |
| Not rebasing before PR | `git rebase origin/main` |
| Merging with failing CI | Fix CI first |
| Not deleting merged branches | Delete after merge |

---

## Related Documents

- [Code Review Guidelines](code-review-guidelines.md)
- [Coding Standards](../development/coding-standards.md)
