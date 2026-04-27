# Cart Module

## Purpose

This document describes the **Cart** module of the CYNA Platform. This module manages the shopping cart for authenticated users — adding products, adjusting quantities and billing cycles, and checking out to initiate the order process.

---

## Overview

| Property | Value |
|----------|-------|
| Module name | `cart` |
| Bounded context | Shopping Cart |
| Base package | `com.cyna.modules.cart` |
| Database schema | `cart_schema` |
| Primary aggregate | `Cart` |

The Cart module handles the pre-order experience. An authenticated user has a single active cart at any time. Lines can be added, modified, or removed. When the user checks out, the cart transitions to `CHECKED_OUT` status, and an order is created in the Order module. The cart computes totals (HT, VAT, TTC) dynamically based on product pricing.

---

## Domain Model

### Aggregate: Cart

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique cart identifier |
| `userId` | `UUID` | Reference to the authenticated user |
| `status` | `CartStatus` | Current state |
| `lines` | `List<CartLine>` | Cart items (immutable list) |
| `createdAt` | `Instant` | Creation timestamp |
| `updatedAt` | `Instant` | Last modification timestamp |

**Behaviors:**

| Method | Description |
|--------|-------------|
| `createForUser(userId)` | Factory — creates an empty active cart for an authenticated user |
| `addOrMergeLine(productId, productName, productCategory, billingCycle, quantity)` | Adds a new line or merges quantity into existing line (same product + billing cycle) |
| `updateLineQuantity(lineId, quantity)` | Updates the quantity of an existing line |
| `removeLine(lineId)` | Removes a line from the cart |
| `changeLineBillingCycle(lineId, newCycle)` | Changes the billing cycle of a line (merges if duplicate) |
| `markCheckedOut()` | Transitions cart to `CHECKED_OUT` status |
| `calculateTotals(pricings, vatRate)` | Computes subtotal HT, VAT amount, and total TTC from product pricing |

### Entity: CartLine

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique line identifier |
| `productId` | `UUID` | Reference to the product |
| `productName` | `String` | Snapshot of product name |
| `productCategory` | `String` | Snapshot of product category |
| `billingCycle` | `BillingCycle` | Selected billing cycle |
| `quantity` | `int` | Quantity (min 1, max 99) |

### Value Objects & Enums

| Value Object | Description | Values / Rules |
|-------------|-------------|----------------|
| `CartStatus` | Current cart state | Enum: `ACTIVE`, `CHECKED_OUT` |
| `BillingCycle` | Pricing frequency | Enum: `MONTHLY`, `ANNUAL` |
| `CartProductPricing` | Product pricing data from Product module | productId, monthlyPrice, annualPrice, currency, published |
| `CartTotals` | Computed totals | subtotalHt, vatAmount, totalTtc, currency |

### Repository Interface

| Method | Description |
|--------|-------------|
| `save(Cart)` | Persist a cart |
| `findById(UUID)` | Find cart by ID |
| `findActiveByUserId(UUID)` | Find the active cart for a user |
| `findLatestByUserId(UUID)` | Find the most recent cart for a user |
| `deleteById(UUID)` | Delete a cart |

---

## Application Layer

### Commands

#### `AddCartLineCommand` → `AddCartLineCommandHandler`

Adds a product to the cart (or merges quantity if same product + billing cycle already exists). Creates a new cart if none exists for the user.

| Field | Type | Validation |
|-------|------|------------|
| `userId` | `UUID` | Required |
| `productId` | `UUID` | Required, must exist and be published in Product Catalog |
| `billingCycle` | `BillingCycle` | Required (`MONTHLY` or `ANNUAL`) |
| `quantity` | `int` | Required, between 1 and 99 |

**Returns:** `Result<CartReadModel>`

#### `UpdateCartLineQuantityCommand` → `UpdateCartLineQuantityCommandHandler`

Updates the quantity of an existing cart line.

| Field | Type | Validation |
|-------|------|------------|
| `userId` | `UUID` | Required |
| `lineId` | `UUID` | Required, must exist in the user's active cart |
| `quantity` | `int` | Required, between 1 and 99 |

**Returns:** `Result<CartReadModel>`

#### `UpdateCartLineBillingCycleCommand` → `UpdateCartLineBillingCycleCommandHandler`

Changes the billing cycle of a cart line.

| Field | Type | Validation |
|-------|------|------------|
| `userId` | `UUID` | Required |
| `lineId` | `UUID` | Required, must exist in the user's active cart |
| `billingCycle` | `BillingCycle` | Required (`MONTHLY` or `ANNUAL`) |

**Returns:** `Result<CartReadModel>`

#### `RemoveCartLineCommand` → `RemoveCartLineCommandHandler`

Removes a line from the cart.

| Field | Type | Validation |
|-------|------|------------|
| `userId` | `UUID` | Required |
| `lineId` | `UUID` | Required, must exist in the user's active cart |

**Returns:** `Result<CartReadModel>`

#### `CheckoutCartCommand` → `CheckoutCartCommandHandler`

Checks out the cart — transitions to `CHECKED_OUT` and initiates order creation.

| Field | Type | Validation |
|-------|------|------------|
| `userId` | `UUID` | Required, must have an active non-empty cart |

**Returns:** `Result<CheckoutCartReadModel>` — cartId, subtotalHt, vatAmount, totalTtc, currency

### Queries

#### `GetCartQuery` → `GetCartQueryHandler`

Retrieves the active cart for the authenticated user, including computed totals and product availability.

| Field | Type |
|-------|------|
| `userId` | `UUID` |

**Returns:** `CartReadModel` — cartId, userId, status, lines, totals, checkoutAllowed, errors

### Application Services

| Service | Description |
|---------|-------------|
| `CartAccessService` | Validates that the authenticated user owns the cart |
| `CartReadModelService` | Builds `CartReadModel` by enriching cart data with product pricing and computing totals |

---

## REST Endpoints

### Customer-Facing

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/cart` | CUSTOMER | Get the active cart with computed totals |
| `POST` | `/api/v1/cart/lines` | CUSTOMER | Add a product to the cart |
| `PATCH` | `/api/v1/cart/lines/{lineId}/quantity` | CUSTOMER | Update line quantity |
| `PATCH` | `/api/v1/cart/lines/{lineId}/billing-cycle` | CUSTOMER | Change line billing cycle |
| `DELETE` | `/api/v1/cart/lines/{lineId}` | CUSTOMER | Remove a line from the cart |
| `POST` | `/api/v1/cart/checkout` | CUSTOMER | Checkout the cart |

### Request Bodies

**`POST /api/v1/cart/lines`** — Add Cart Line

| Field | Type | Validation |
|-------|------|------------|
| `productId` | `UUID` | Required |
| `billingCycle` | `String` | Required (`MONTHLY` or `ANNUAL`) |
| `quantity` | `int` | Required, min 1, max 99 |

**`PATCH /api/v1/cart/lines/{lineId}/quantity`** — Update Quantity

| Field | Type | Validation |
|-------|------|------------|
| `quantity` | `int` | Required, min 1, max 99 |

**`PATCH /api/v1/cart/lines/{lineId}/billing-cycle`** — Update Billing Cycle

| Field | Type | Validation |
|-------|------|------------|
| `billingCycle` | `String` | Required (`MONTHLY` or `ANNUAL`) |

### Response Bodies

**`CartResponse`**

| Field | Type |
|-------|------|
| `cartId` | `UUID` |
| `userId` | `UUID` |
| `status` | `String` |
| `lines` | `List<CartLineResponse>` |
| `totals` | `CartTotalsResponse` |
| `checkoutAllowed` | `boolean` |
| `errors` | `List<String>` |

**`CartLineResponse`**

| Field | Type |
|-------|------|
| `lineId` | `UUID` |
| `productId` | `UUID` |
| `productName` | `String` |
| `productCategory` | `String` |
| `billingCycle` | `String` |
| `quantity` | `int` |
| `unitPrice` | `BigDecimal` |
| `currency` | `String` |
| `available` | `boolean` |

**`CartTotalsResponse`**

| Field | Type |
|-------|------|
| `subtotalHt` | `BigDecimal` |
| `vatAmount` | `BigDecimal` |
| `totalTtc` | `BigDecimal` |
| `currency` | `String` |

**`CheckoutCartResponse`**

| Field | Type |
|-------|------|
| `cartId` | `UUID` |
| `subtotalHt` | `BigDecimal` |
| `vatAmount` | `BigDecimal` |
| `totalTtc` | `BigDecimal` |
| `currency` | `String` |

---

## Module Dependencies

| Direction | Module | Mechanism | Description |
|-----------|--------|-----------|-------------|
| **Depends on** | Product Catalog | `ProductQueryApi` or pricing service (sync) | Retrieves product pricing (monthly/annual) and availability |
| **Depends on** | User (IAM) | User identity | Authenticated user identity via `@AuthenticationPrincipal` |
| **Provides to** | Order | Checkout flow (sync) | Checkout creates an order from the cart lines |

---

## Business Rules

| Rule | Enforcement |
|------|-------------|
| Only active carts can be modified | Status check in all mutation methods |
| Quantity must be between 1 and 99 | `CartLine` constructor guard + request DTO validation |
| Adding the same product + billing cycle merges quantity | `addOrMergeLine()` logic |
| Merged quantity cannot exceed 99 | Check in `addOrMergeLine()` |
| Each user has at most one active cart | `findActiveByUserId()` returns single cart |
| Checkout requires at least one line | Checked in `checkout()` |
| Cart cannot be checked out twice | Status check (`ACTIVE` → `CHECKED_OUT`) |
| Product must be published to be added | Product availability check via Product module |
| Totals are computed dynamically (not stored) | `CartReadModelService` computes HT, VAT, TTC from product pricing |

---

## Related Documents

- [Module Structure](../architecture/module-structure.md)
- [Product Catalog Module](product-module.md)
- [Order Module](order-module.md)
- [Frontend Architecture](../architecture/frontend-architecture.md)
