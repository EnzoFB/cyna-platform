# Database Migrations

## Purpose

This document defines the database migration strategy using **Flyway**. All schema changes are version-controlled, reviewed, and applied consistently across environments.

---

## Overview

Flyway manages database schema evolution through versioned SQL migration scripts. Each script runs exactly once and is tracked in a `flyway_schema_history` table.

**Key principle**: The database schema is always reproducible from scratch by running all migrations in order.

---

## Migration File Structure

```
src/main/resources/db/migration/
├── V1__create_user_schema.sql
├── V2__create_product_schema.sql
├── V3__create_order_schema.sql
├── V4__create_payment_schema.sql
├── V5__add_user_roles_table.sql
├── V6__add_product_categories.sql
├── V7__add_order_lines_table.sql
├── V8__add_refresh_tokens_table.sql
└── ...
```

---

## Naming Convention

### Versioned Migrations

Format: `V{version}__{description}.sql`

| Part | Rule | Example |
|------|------|---------|
| Prefix | `V` for versioned | `V` |
| Version | Sequential integer | `1`, `2`, `10`, `100` |
| Separator | Double underscore `__` | `__` |
| Description | Snake_case, descriptive | `create_user_schema`, `add_email_index` |
| Extension | `.sql` | `.sql` |

**Examples:**

```
V1__create_user_schema.sql
V2__create_product_schema.sql
V3__add_index_on_users_email.sql
V4__add_cancellation_reason_to_orders.sql
V5__create_refresh_tokens_table.sql
```

### Repeatable Migrations (Rare)

Format: `R__{description}.sql`

Used for views, functions, or stored procedures that can be re-applied:

```
R__create_updated_at_trigger_function.sql
```

Repeatable migrations run every time their checksum changes.

---

## Migration Content Rules

### 1. One Logical Change Per Migration

Each migration file should contain one logical schema change:

```sql
-- ✅ CORRECT: V5__add_email_index.sql
-- Single logical change
CREATE UNIQUE INDEX uq_users_email ON user_schema.users (email);
```

```sql
-- ❌ WRONG: Mixing unrelated changes
CREATE UNIQUE INDEX uq_users_email ON user_schema.users (email);
ALTER TABLE order_schema.orders ADD COLUMN notes TEXT;
-- These should be separate migrations
```

### 2. Always Use Schema-Qualified Names

```sql
-- ✅ CORRECT
CREATE TABLE order_schema.orders ( ... );
CREATE INDEX idx_orders_status ON order_schema.orders (status);

-- ❌ WRONG
CREATE TABLE orders ( ... );  -- Ambiguous — which schema?
```

### 3. Always Include IF NOT EXISTS / IF EXISTS

```sql
-- ✅ CORRECT
CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE TABLE IF NOT EXISTS user_schema.users ( ... );
CREATE INDEX IF NOT EXISTS idx_users_email ON user_schema.users (email);

-- For destructive operations
DROP INDEX IF EXISTS idx_old_index;
ALTER TABLE user_schema.users DROP COLUMN IF EXISTS legacy_column;
```

### 4. Always Be Backwards Compatible

Migrations must not break the currently running application. This enables zero-downtime deployments.

| Operation | Safe? | Strategy |
|-----------|-------|----------|
| Add column (nullable) | ✅ Yes | Old code ignores new column |
| Add column (non-null with default) | ✅ Yes | Old code ignores new column |
| Add index | ✅ Yes | Transparent to application |
| Add table | ✅ Yes | Old code doesn't reference it |
| Remove column | ⚠️ Careful | First deploy code that doesn't use it, then drop |
| Rename column | ❌ No | Add new column, migrate data, drop old (multi-step) |
| Change column type | ❌ No | Add new column, migrate data, drop old (multi-step) |
| Drop table | ⚠️ Careful | Ensure no code references it first |

### 5. Use Explicit Constraints

```sql
-- ✅ CORRECT: Named constraints
ALTER TABLE order_schema.orders
    ADD CONSTRAINT ck_orders_total_amount_positive CHECK (total_amount >= 0);

-- ❌ WRONG: Anonymous constraint
ALTER TABLE order_schema.orders ADD CHECK (total_amount >= 0);
-- Cannot be easily referenced or dropped later
```

---

## Migration Examples

### Create Schema and Initial Table

```sql
-- V1__create_user_schema.sql

CREATE SCHEMA IF NOT EXISTS user_schema;

CREATE TABLE IF NOT EXISTS user_schema.users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    role            VARCHAR(50)     NOT NULL DEFAULT 'CUSTOMER',
    status          VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'ADMIN', 'SUPPORT')),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_users_email ON user_schema.users (email);
CREATE INDEX idx_users_role ON user_schema.users (role);
CREATE INDEX idx_users_created_at ON user_schema.users (created_at DESC);
```

### Add Column

```sql
-- V10__add_phone_number_to_users.sql

ALTER TABLE user_schema.users
    ADD COLUMN phone_number VARCHAR(20) NULL;

COMMENT ON COLUMN user_schema.users.phone_number IS 'User phone number in E.164 format';
```

### Add Index

```sql
-- V11__add_index_orders_created_at.sql

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_created_at_desc
    ON order_schema.orders (created_at DESC);
```

> **Note**: Use `CONCURRENTLY` for indexes on large tables to avoid locking.

### Data Migration

```sql
-- V12__migrate_status_values.sql

-- Migrate old status values to new ones
UPDATE order_schema.orders
SET status = 'CONFIRMED'
WHERE status = 'APPROVED';

-- Update constraint to reflect new values
ALTER TABLE order_schema.orders DROP CONSTRAINT IF EXISTS ck_orders_status;
ALTER TABLE order_schema.orders
    ADD CONSTRAINT ck_orders_status
    CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID', 'FULFILLED', 'CANCELLED'));
```

---

## Flyway Configuration

### application.yml

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    schemas:
      - user_schema
      - product_schema
      - order_schema
      - payment_schema
    baseline-on-migrate: false
    validate-on-migrate: true
    clean-disabled: true  # NEVER allow clean in production
```

### Environment-Specific Settings

| Environment | `clean-disabled` | `baseline-on-migrate` |
|-------------|------------------|-----------------------|
| Local | `false` | `true` |
| Test | `false` | `true` |
| Staging | `true` | `false` |
| Production | `true` | `false` |

---

## Workflow

### Creating a New Migration

1. Determine the next version number: `./gradlew flywayInfo`
2. Create the file: `src/main/resources/db/migration/V{next}__description.sql`
3. Write the SQL.
4. Run locally: `./gradlew flywayMigrate`
5. Verify: `./gradlew flywayInfo`
6. Test: Run integration tests.
7. Commit: Include the migration in your feature branch.
8. Review: Migration must be reviewed in the pull request.

### Handling Mistakes

**If a migration has NOT been applied to any shared environment:**

- Delete or fix the migration file.
- Run `./gradlew flywayClean flywayMigrate` locally.

**If a migration HAS been applied to staging/production:**

- **Never modify it.** Create a corrective migration with the next version number.
- Example: If `V10` had a bug, create `V11__fix_v10_issue.sql`.

---

## Rules

| Rule | Rationale |
|------|-----------|
| Never modify a migration after it's merged to `main` | Flyway validates checksums — changes cause failures |
| One logical change per migration | Easier to review, revert, and understand |
| Always use schema-qualified table names | Prevents ambiguity |
| Use `IF NOT EXISTS` / `IF EXISTS` | Makes migrations idempotent-safe |
| Name all constraints explicitly | Enables easy reference and removal |
| Use `CONCURRENTLY` for indexes on large tables | Avoids locking |
| Migrations must be backwards compatible | Enables zero-downtime deployments |
| Never use `flywayClean` in staging or production | Destroys all data |
| Always review migrations in PRs | Schema changes are critical |
| Test migrations against a real PostgreSQL (Testcontainers) | Validates SQL syntax and semantics |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Editing an already-applied migration | Create a new corrective migration |
| Skipping version numbers (V1, V3) | Use sequential versions without gaps |
| Using `flywayClean` in production | Set `clean-disabled: true` |
| Not using `CONCURRENTLY` for large table indexes | Add `CONCURRENTLY` to avoid downtime |
| Unnamed constraints | Always name constraints explicitly |
| Missing `IF NOT EXISTS` | Include for idempotency |
| Data migration without testing | Test with realistic data volumes |
| Not reviewing migrations in PRs | Treat schema changes as critical code |

---

## Related Documents

- [Database Guidelines](database-guidelines.md)
- [Development Setup](../development/development-setup.md)
- [Backend Architecture](../architecture/backend-architecture.md)
