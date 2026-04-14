# Product Catalog Module

## Purpose

This document describes the **Product Catalog** module of the CYNA Platform. This module manages the catalog of cybersecurity service offerings (SOC, EDR, XDR), their pricing, categorization, and publication lifecycle.

---

## Overview

| Property | Value |
|----------|-------|
| Module name | `product` |
| Bounded context | Product Catalog |
| Base package | `com.cyna.modules.product` |
| Database schema | `product_schema` |
| Primary aggregates | `Product`, `Category` |

The Product Catalog module is the system of record for all purchasable cybersecurity services. It owns the complete product definition — name, descriptions, dual pricing (monthly and annual), category, priority, and availability status. It also manages product categories as a separate aggregate with their own lifecycle. Other modules (notably Cart) consume product data through the module's `ProductQueryApi` interface.

---

## Domain Model

### Aggregate: Product

| Field | Type | Description |
|-------|------|-------------|
| `id` | `ProductId` | Unique product identifier |
| `name` | `ProductName` | Display name |
| `description` | `String` | Detailed product description |
| `category` | `Category` | Service classification |
| `price` | `Money` | Current price with currency |
| `billingCycle` | `BillingCycle` | Pricing frequency |
| `features` | `List<Feature>` | List of technical capabilities |
| `availability` | `Availability` | Publication status (defaults to `UNPUBLISHED`) |
| `createdAt` | `Instant` | Creation timestamp |
| `updatedAt` | `Instant` | Last modification timestamp |

**Behaviors:**

| Method | Description |
|--------|-------------|
| `create(...)` | Factory method — creates a new product and raises `ProductCreated` |
| `publish()` | Sets availability to `PUBLISHED`, raises `ProductPublished` |
| `unpublish()` | Sets availability to `UNPUBLISHED`, raises `ProductUnpublished` |
| `changePrice(newPrice)` | Updates price, raises `PriceChanged` |
| `update(...)` | Updates name, description, category, billing cycle, features; raises `ProductUpdated` |

### Value Objects

| Value Object | Description | Validation Rules |
|-------------|-------------|------------------|
| `ProductId` | Unique product identifier | Non-null UUID |
| `ProductName` | Product display name | Non-blank, max 255 characters |
| `Category` | Service classification | Enum: `ENDPOINT`, `NETWORK`, `CLOUD` |
| `Money` | Price with currency | Non-null, non-negative amount; non-blank currency |
| `Feature` | Technical capability | Non-blank name and description |
| `BillingCycle` | Pricing frequency | Enum: `MONTHLY`, `QUARTERLY`, `ANNUAL` |
| `Availability` | Publication status | Enum: `PUBLISHED`, `UNPUBLISHED` |

### Repository Interface

| Method | Description |
|--------|-------------|
| `save(Product)` | Persist a product |
| `findById(ProductId)` | Find product by ID |
| `findAllByIds(List<ProductId>)` | Find multiple products by IDs |
| `findAll()` | List all products |
| `findAllPublished()` | List published products only |
| `existsById(ProductId)` | Check if product exists |
| `deleteById(ProductId)` | Delete a product |

---

## Domain Events

| Event | Trigger | Payload |
|-------|---------|---------|
| `ProductCreated` | Admin creates a new product | productId, name, amount, currency, category, occurredAt |
| `ProductUpdated` | Admin updates product details | productId, occurredAt |
| `ProductPublished` | Admin publishes a product | productId, occurredAt |
| `ProductUnpublished` | Admin unpublishes a product | productId, occurredAt |
| `PriceChanged` | Admin changes product price | productId, oldAmount, oldCurrency, newAmount, newCurrency, occurredAt |

---

## Application Layer

### Commands

#### `CreateProductCommand` → `CreateProductCommandHandler`

Creates a new product in the catalog.

| Field | Type | Validation |
|-------|------|------------|
| `name` | `String` | Required, non-blank |
| `description` | `String` | Required, non-blank |
| `category` | `String` | Required, must match `Category` enum |
| `price` | `BigDecimal` | Required, positive |
| `currency` | `String` | Required, non-blank |
| `billingCycle` | `String` | Required, must match `BillingCycle` enum |
| `features` | `List<FeatureDto>` | Required, each with name + description |

**Returns:** `Result<ProductId>`

#### `UpdateProductCommand` → `UpdateProductCommandHandler`

Updates product details (excluding price).

| Field | Type | Validation |
|-------|------|------------|
| `productId` | `UUID` | Required |
| `name` | `String` | Required, non-blank |
| `description` | `String` | Required, non-blank |
| `category` | `String` | Required |
| `billingCycle` | `String` | Required |
| `features` | `List<FeatureDto>` | Required |

**Returns:** `Result<Void>`

#### `PublishProductCommand` → `PublishProductCommandHandler`

Makes a product visible and purchasable.

| Field | Type | Validation |
|-------|------|------------|
| `productId` | `UUID` | Required |

**Returns:** `Result<Void>`

#### `UnpublishProductCommand` → `UnpublishProductCommandHandler`

Removes a product from the storefront.

| Field | Type | Validation |
|-------|------|------------|
| `productId` | `UUID` | Required |

**Returns:** `Result<Void>`

#### `ChangePriceCommand` → `ChangePriceCommandHandler`

Updates the product price.

| Field | Type | Validation |
|-------|------|------------|
| `productId` | `UUID` | Required |
| `price` | `BigDecimal` | Required, positive |
| `currency` | `String` | Required, non-blank |

**Returns:** `Result<Void>`

#### `DeleteProductCommand` → `DeleteProductCommandHandler`

Removes a product from the catalog.

| Field | Type | Validation |
|-------|------|------------|
| `productId` | `UUID` | Required |

**Returns:** `Result<Void>`

### Queries

#### `GetProductByIdQuery` → `GetProductByIdQueryHandler`

Retrieves a single product by ID.

| Field | Type |
|-------|------|
| `productId` | `UUID` |

**Returns:** `ProductReadModel` — id, name, description, category, price, currency, billingCycle, features, availability, createdAt, updatedAt

#### `ListProductsQuery` → `ListProductsQueryHandler`

Lists all products with optional filtering.

| Field | Type |
|-------|------|
| `category` | `String` (optional) |
| `availability` | `String` (optional) |

**Returns:** `List<ProductReadModel>`

#### `ListPublishedProductsQuery` → `ListPublishedProductsQueryHandler`

Lists only published products (storefront). No input fields.

**Returns:** `List<ProductReadModel>`

### Public API (Inter-Module)

The Product module exposes a `ProductQueryApi` for other modules to consume product data synchronously:

| Method | Returns | Description |
|--------|---------|-------------|
| `getById(ProductId)` | `Optional<ProductInfo>` | Get a product by ID (id, name, description, price, currency) |
| `getByIds(List<ProductId>)` | `List<ProductInfo>` | Get multiple products by IDs |
| `exists(ProductId)` | `boolean` | Check if a product exists |

**Consumers:** Order Management module uses `ProductQueryApi` to validate products and retrieve pricing when creating orders.

---

## REST Endpoints

### Customer-Facing (Public)

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/products` | Public | List published products (storefront) |
| `GET` | `/api/v1/products/{id}` | Public | Get product details |

### Backoffice (Admin)

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/admin/products` | ADMIN | Create a new product |
| `GET` | `/api/v1/admin/products` | ADMIN, SUPPORT | List all products (including unpublished) |
| `GET` | `/api/v1/admin/products/{id}` | ADMIN, SUPPORT | Get product details |
| `PUT` | `/api/v1/admin/products/{id}` | ADMIN | Update product details |
| `DELETE` | `/api/v1/admin/products/{id}` | ADMIN | Delete a product |
| `POST` | `/api/v1/admin/products/{id}/publish` | ADMIN | Publish a product |
| `POST` | `/api/v1/admin/products/{id}/unpublish` | ADMIN | Unpublish a product |
| `PUT` | `/api/v1/admin/products/{id}/price` | ADMIN | Change product price |

### Request Bodies

**`POST /api/v1/admin/products`** — Create Product

| Field | Type | Validation |
|-------|------|------------|
| `name` | `String` | Required, non-blank |
| `description` | `String` | Required, non-blank |
| `category` | `String` | Required (`ENDPOINT`, `NETWORK`, `CLOUD`) |
| `price` | `BigDecimal` | Required, positive |
| `currency` | `String` | Required, non-blank |
| `billingCycle` | `String` | Required (`MONTHLY`, `QUARTERLY`, `ANNUAL`) |
| `features` | `List<{name, description}>` | Required |

**`PUT /api/v1/admin/products/{id}`** — Update Product

| Field | Type | Validation |
|-------|------|------------|
| `name` | `String` | Required, non-blank |
| `description` | `String` | Required, non-blank |
| `category` | `String` | Required |
| `billingCycle` | `String` | Required |
| `features` | `List<{name, description}>` | Required |

**`PUT /api/v1/admin/products/{id}/price`** — Change Price

| Field | Type | Validation |
|-------|------|------------|
| `price` | `BigDecimal` | Required, positive |
| `currency` | `String` | Required, non-blank |

### Response Body — `ProductResponse`

| Field | Type |
|-------|------|
| `id` | `UUID` |
| `name` | `String` |
| `description` | `String` |
| `category` | `String` |
| `price` | `BigDecimal` |
| `currency` | `String` |
| `billingCycle` | `String` |
| `features` | `List<{name, description}>` |
| `availability` | `String` |
| `createdAt` | `Instant` |
| `updatedAt` | `Instant` |

---

## Module Dependencies

| Direction | Module | Mechanism | Description |
|-----------|--------|-----------|-------------|
| **Provides to** | Order | `ProductQueryApi` (sync) | Order module queries product data when creating orders |
| **Provides to** | Notification | Domain events (async) | Notification may react to product changes |
| **Depends on** | User (IAM) | User identity | Admin identity for authorization |

---

## Business Rules

| Rule | Enforcement |
|------|-------------|
| Product name must be unique | Application layer check + DB unique constraint |
| Price must be positive | Value object constructor (`Money`) |
| Only ADMIN can create, update, or delete products | Spring Security + RBAC |
| Only published products are visible to customers | Query filter in storefront endpoints |
| Price changes are audited via `PriceChanged` event | Domain event raised on price change |
| Unpublishing a product does not affect existing orders | Orders reference product data at order-creation time |

---

## Related Documents

- [Bounded Contexts](../domain/bounded-contexts.md)
- [Module Structure](../architecture/module-structure.md)
- [Inter-Module Communication](../architecture/inter-module-communication.md)
- [API Guidelines](../api/api-guidelines.md)
- [Database Guidelines](../database/database-guidelines.md)
