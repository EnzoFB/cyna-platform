# Database Guidelines

## Purpose

This document defines the database design conventions, naming standards, indexing strategy, and best practices for the CYNA Platform. PostgreSQL is the sole database engine.

---

## Schema Design

### Per-Module Schemas

Each module has its own PostgreSQL schema. This enforces data ownership and prevents cross-module table access:

| Module | Schema Name |
|--------|-------------|
| User | `user_schema` |
| Product | `product_schema` |
| Order | `order_schema` |
| Payment | `payment_schema` |
| Notification | `notification_schema` |

**Rule**: A module must **never** query or join tables from another module's schema. Cross-module data access is done via public APIs.

```sql
-- Create schema per module
CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE SCHEMA IF NOT EXISTS product_schema;
CREATE SCHEMA IF NOT EXISTS order_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;
```

---

## Naming Conventions

### Tables

| Convention | Rule | Example |
|-----------|------|---------|
| Format | `snake_case`, plural | `orders`, `order_lines`, `products` |
| Prefix | None — the schema provides context | `order_schema.orders`, not `order_schema.order_orders` |
| Join tables | `{table1}_{table2}` alphabetical | `product_categories` |

### Columns

| Convention | Rule | Example |
|-----------|------|---------|
| Format | `snake_case` | `created_at`, `customer_id`, `total_amount` |
| Primary key | `id` (UUID) | `id UUID PRIMARY KEY` |
| Foreign key | `{referenced_table_singular}_id` | `customer_id`, `product_id` |
| Boolean | `is_` or `has_` prefix | `is_active`, `has_subscription` |
| Timestamps | `_at` suffix | `created_at`, `updated_at`, `deleted_at` |
| Enum/status | Stored as `VARCHAR` | `status VARCHAR(50) NOT NULL` |

### Indexes

| Convention | Rule | Example |
|-----------|------|---------|
| Format | `idx_{table}_{columns}` | `idx_orders_customer_id` |
| Unique | `uq_{table}_{columns}` | `uq_users_email` |
| Partial | `idx_{table}_{columns}_{condition}` | `idx_orders_status_pending` |

### Constraints

| Convention | Rule | Example |
|-----------|------|---------|
| Primary key | `pk_{table}` | `pk_orders` |
| Foreign key | `fk_{table}_{referenced_table}` | `fk_orders_customers` |
| Check | `ck_{table}_{column}` | `ck_orders_total_amount_positive` |
| Unique | `uq_{table}_{columns}` | `uq_users_email` |

---

## Column Types

| Data | PostgreSQL Type | Example |
|------|----------------|---------|
| Identifiers | `UUID` | `id UUID PRIMARY KEY DEFAULT gen_random_uuid()` |
| Short text | `VARCHAR(n)` | `name VARCHAR(200) NOT NULL` |
| Long text | `TEXT` | `description TEXT` |
| Money/decimal | `NUMERIC(19,4)` | `amount NUMERIC(19,4) NOT NULL` |
| Currency | `VARCHAR(3)` | `currency VARCHAR(3) NOT NULL DEFAULT 'EUR'` |
| Status/enum | `VARCHAR(50)` | `status VARCHAR(50) NOT NULL` |
| Boolean | `BOOLEAN` | `is_active BOOLEAN NOT NULL DEFAULT true` |
| Timestamp | `TIMESTAMPTZ` | `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` |
| Date | `DATE` | `birth_date DATE` |
| Integer count | `INTEGER` | `quantity INTEGER NOT NULL` |
| JSON data | `JSONB` | `metadata JSONB` |

**Rules:**
- Always use `TIMESTAMPTZ` (with time zone), never `TIMESTAMP`.
- Always use `NUMERIC` for money, never `FLOAT` or `DOUBLE`.
- Always use `UUID` for primary keys, never auto-increment integers.
- Always use `JSONB`, never `JSON` (JSONB is indexable and more efficient).

---

## Table Template

```sql
CREATE TABLE IF NOT EXISTS order_schema.orders (
    -- Primary Key
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Business Fields
    customer_id       UUID            NOT NULL,
    status            VARCHAR(50)     NOT NULL DEFAULT 'PENDING',
    total_amount      NUMERIC(19,4)   NOT NULL,
    currency          VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    cancellation_reason TEXT          NULL,

    -- Audit Fields
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT ck_orders_total_amount_positive CHECK (total_amount >= 0),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID', 'FULFILLED', 'CANCELLED'))
);

-- Indexes
CREATE INDEX idx_orders_customer_id ON order_schema.orders (customer_id);
CREATE INDEX idx_orders_status ON order_schema.orders (status);
CREATE INDEX idx_orders_created_at ON order_schema.orders (created_at DESC);
```

---

## Indexing Strategy

### When to Create Indexes

| Create an index when... | Example |
|------------------------|---------|
| Column is used in `WHERE` clauses | `status`, `customer_id` |
| Column is used in `JOIN` conditions | Foreign key columns |
| Column is used in `ORDER BY` | `created_at DESC` |
| Column has a uniqueness constraint | `email` |
| Column is used in frequent lookups | `order_id` in `order_lines` |

### When NOT to Create Indexes

| Don't create an index when... | Reason |
|------------------------------|--------|
| Table is small (< 1000 rows) | Full scan is faster |
| Column has very low cardinality | `boolean` columns with 50/50 distribution |
| Column is rarely queried | Indexes have write overhead |
| Table is write-heavy with rare reads | Index maintenance slows writes |

### Index Types

| Type | Use Case | Syntax |
|------|----------|--------|
| B-tree (default) | Equality and range queries | `CREATE INDEX idx_name ON table (column)` |
| Partial | Filtering on subset of rows | `CREATE INDEX idx_name ON table (col) WHERE condition` |
| Composite | Multi-column queries | `CREATE INDEX idx_name ON table (col1, col2)` |
| GIN | JSONB and array fields | `CREATE INDEX idx_name ON table USING GIN (json_col)` |
| Unique | Uniqueness enforcement | `CREATE UNIQUE INDEX uq_name ON table (column)` |

### Partial Index Example

```sql
-- Only index pending orders (frequently queried status)
CREATE INDEX idx_orders_status_pending
    ON order_schema.orders (created_at DESC)
    WHERE status = 'PENDING';
```

### Composite Index Example

```sql
-- Covers the query: WHERE customer_id = ? ORDER BY created_at DESC
CREATE INDEX idx_orders_customer_created
    ON order_schema.orders (customer_id, created_at DESC);
```

---

## Soft Delete

For entities that should not be permanently deleted:

```sql
ALTER TABLE user_schema.users ADD COLUMN deleted_at TIMESTAMPTZ NULL;

-- Queries must filter by deleted_at
CREATE INDEX idx_users_active ON user_schema.users (email) WHERE deleted_at IS NULL;
```

Application code must filter `WHERE deleted_at IS NULL` for all queries on soft-deletable entities.

---

## Audit Columns

Every table must have:

| Column | Type | Purpose |
|--------|------|---------|
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | Record creation timestamp |
| `updated_at` | `TIMESTAMPTZ NOT NULL DEFAULT NOW()` | Last modification timestamp |

The `updated_at` column is updated by the application (JPA `@PreUpdate`) or by a database trigger:

```sql
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON order_schema.orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
```

---

## Foreign Keys Across Schemas

**Cross-schema foreign keys are NOT allowed.** Each module manages its own referential integrity.

If the `orders` table needs to reference a `customer_id`, it stores the UUID but does **not** create a `FOREIGN KEY` constraint to the `users` table in `user_schema`. The application layer enforces referential integrity via public APIs.

```sql
-- ✅ CORRECT: Store the UUID without FK constraint
customer_id UUID NOT NULL  -- References user, but no FK constraint

-- ❌ WRONG: Cross-schema foreign key
FOREIGN KEY (customer_id) REFERENCES user_schema.users(id)
```

---

## Performance Guidelines

| Guideline | Rationale |
|-----------|-----------|
| Use connection pooling (HikariCP) | Avoid connection overhead |
| Set appropriate pool size (CPU cores × 2 + disk spindles) | Optimal concurrency |
| Use `EXPLAIN ANALYZE` to verify query plans | Ensure indexes are used |
| Avoid `SELECT *` in queries | Fetch only needed columns |
| Use batch inserts for bulk operations | Reduce round-trips |
| Monitor slow queries with `pg_stat_statements` | Identify bottlenecks |
| Vacuum regularly (autovacuum is on by default) | Prevent table bloat |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Using auto-increment IDs | Use UUIDs |
| Using `TIMESTAMP` without time zone | Use `TIMESTAMPTZ` |
| Using `FLOAT` for money | Use `NUMERIC(19,4)` |
| Cross-schema foreign keys | Store UUID without FK constraint |
| Missing indexes on foreign key columns | Always index FK columns |
| No index on columns used in WHERE/ORDER BY | Add targeted indexes |
| Over-indexing (index on every column) | Index only frequently queried columns |
| Missing audit columns | Every table needs `created_at` and `updated_at` |

---

## Related Documents

- [Migrations](migrations.md)
- [Backend Architecture](../architecture/backend-architecture.md)
- [Module Structure](../architecture/module-structure.md)
