# Order Management Module

> **Pour comprendre le parcours complet (commande → paiement → abonnement → renouvellement → annulation)** avec tous les diagrammes Mermaid (séquences, états, routage webhook), voir **[Orders & Payments — flux complet](../flows/order-payment-flow.md)**. Ce document décrit l'intérieur du module Order uniquement.

## Purpose

This document describes the **Order Management** module of the CYNA Platform. This module manages the complete lifecycle of customer orders — from creation through confirmation, payment, fulfillment, and cancellation.

---

## Overview

| Property | Value |
|----------|-------|
| Module name | `order` |
| Bounded context | Order Management |
| Base package | `com.cyna.modules.order` |
| Database schema | `order_schema` |
| Primary aggregate | `Order` |

The Order Management module captures a customer's intent to purchase cybersecurity services. It coordinates with the Product Catalog module (to retrieve product data), publishes events consumed by the Payment module (to initiate payment processing), and tracks order status throughout the entire lifecycle.

---

## Domain Model

### Aggregate: Order

| Field | Type | Description |
|-------|------|-------------|
| `id` | `OrderId` | Unique order identifier |
| `customerId` | `CustomerId` | Reference to the customer (from IAM) |
| `status` | `OrderStatus` | Current state of the order |
| `lines` | `List<OrderLine>` | Order items (max 50) |
| `totalAmount` | `Money` | Calculated total |
| `cancellationReason` | `String` | Free-text reason (when cancelled) |
| `createdAt` | `Instant` | Creation timestamp |
| `updatedAt` | `Instant` | Last modification timestamp |

**Behaviors:**

| Method | Description |
|--------|-------------|
| `create(customerId, lines)` | Factory method — creates a draft order and raises `OrderCreated` |
| `addLine(productId, productName, quantity, unitPrice)` | Adds a line to a draft order, raises `OrderLineAdded` |
| `submit()` | Transitions from `DRAFT` → `PENDING` |
| `confirm()` | Transitions from `PENDING` → `CONFIRMED`, raises `OrderConfirmed` |
| `markPaid(paymentId)` | Transitions from `CONFIRMED` → `PAID`, raises `OrderPaid` |
| `fulfill()` | Transitions from `PAID` → `FULFILLED`, raises `OrderFulfilled` |
| `cancel(reason)` | Cancels the order (if not already fulfilled), raises `OrderCancelled` |

### Entity: OrderLine

| Field | Type | Description |
|-------|------|-------------|
| `id` | `OrderLineId` | Unique line identifier |
| `productId` | `ProductId` | Reference to the product |
| `productName` | `String` | Snapshot of product name at order time |
| `quantity` | `int` | Quantity ordered |
| `unitPrice` | `Money` | Snapshot of unit price at order time |

### Value Objects

| Value Object | Description | Validation Rules |
|-------------|-------------|------------------|
| `OrderId` | Unique order identifier | Non-null UUID |
| `OrderLineId` | Unique line identifier | Non-null UUID |
| `CustomerId` | Reference to the customer (from IAM) | Non-null UUID |
| `ProductId` | Reference to the product (from Catalog) | Non-null UUID |
| `OrderStatus` | Current state of the order | Enum with valid transitions |
| `Money` | Price with currency (shared value object) | Non-negative amount, non-blank currency |

### Order Status Transitions

```
DRAFT → PENDING → CONFIRMED → PAID → FULFILLED
                                  ↓
  DRAFT → CANCELLED          CANCELLED
  PENDING → CANCELLED
  CONFIRMED → CANCELLED
  PAID → CANCELLED
```

### Repository Interface

| Method | Description |
|--------|-------------|
| `save(Order)` | Persist an order |
| `findById(OrderId)` | Find order by ID |
| `findByCustomerId(CustomerId)` | Find orders for a customer |
| `findAll()` | List all orders |
| `findByStatus(OrderStatus)` | Find orders by status |

---

## Domain Events

| Event | Trigger | Payload |
|-------|---------|---------|
| `OrderCreated` | Customer creates an order | orderId, customerId, totalAmount, currency, occurredAt |
| `OrderLineAdded` | Line added to a draft order | orderId, productId, quantity, occurredAt |
| `OrderConfirmed` | System confirms the order | orderId, occurredAt |
| `OrderPaid` | Payment succeeds | orderId, paymentId, occurredAt |
| `OrderFulfilled` | Services activated | orderId, occurredAt |
| `OrderCancelled` | Customer or admin cancels | orderId, reason, occurredAt |

---

## Application Layer

### Commands

#### `CreateOrderCommand` → `CreateOrderCommandHandler`

Creates a new order with product lines. Uses `ProductQueryApi` to validate products and retrieve pricing.

| Field | Type | Validation |
|-------|------|------------|
| `customerId` | `UUID` | Required |
| `items` | `List<OrderItemDto>` | Required, non-empty |

`OrderItemDto`:

| Field | Type | Validation |
|-------|------|------------|
| `productId` | `UUID` | Required, must exist in Product Catalog |
| `quantity` | `int` | Required, positive |

**Returns:** `Result<OrderId>`

#### `AddOrderLineCommand` → `AddOrderLineCommandHandler`

Adds a line to an existing draft order.

| Field | Type | Validation |
|-------|------|------------|
| `orderId` | `UUID` | Required, must be an existing DRAFT order |
| `productId` | `UUID` | Required, must exist in Product Catalog |
| `quantity` | `int` | Required, positive |

**Returns:** `Result<Void>`

#### `SubmitOrderCommand` → `SubmitOrderCommandHandler`

Submits a draft order for processing.

| Field | Type | Validation |
|-------|------|------------|
| `orderId` | `UUID` | Required, must be DRAFT with at least one line |

**Returns:** `Result<Void>`

#### `ConfirmOrderCommand` → `ConfirmOrderCommandHandler`

Confirms a pending order.

| Field | Type | Validation |
|-------|------|------------|
| `orderId` | `UUID` | Required, must be PENDING |

**Returns:** `Result<Void>`

#### `CancelOrderCommand` → `CancelOrderCommandHandler`

Cancels an order with a reason.

| Field | Type | Validation |
|-------|------|------------|
| `orderId` | `UUID` | Required, must not be FULFILLED or already CANCELLED |
| `reason` | `String` | Required, non-blank, max 500 characters |

**Returns:** `Result<Void>`

#### `MarkOrderPaidCommand` → `MarkOrderPaidCommandHandler`

Marks an order as paid. Triggered internally by payment event handler.

| Field | Type | Validation |
|-------|------|------------|
| `orderId` | `UUID` | Required, must be CONFIRMED |
| `paymentId` | `UUID` | Required |

**Returns:** `Result<Void>`

#### `FulfillOrderCommand` → `FulfillOrderCommandHandler`

Marks an order as fulfilled (services activated).

| Field | Type | Validation |
|-------|------|------------|
| `orderId` | `UUID` | Required, must be PAID |

**Returns:** `Result<Void>`

### Queries

#### `GetOrderByIdQuery` → `GetOrderByIdQueryHandler`

Retrieves a single order by ID.

| Field | Type |
|-------|------|
| `orderId` | `UUID` |

**Returns:** `OrderReadModel` — id, customerId, status, totalAmount, currency, lines, cancellationReason, createdAt, updatedAt

#### `ListCustomerOrdersQuery` → `ListCustomerOrdersQueryHandler`

Lists orders for a specific customer.

| Field | Type |
|-------|------|
| `customerId` | `UUID` |

**Returns:** `List<OrderReadModel>`

#### `ListAllOrdersQuery` → `ListAllOrdersQueryHandler`

Lists all orders (admin/support). No input fields.

**Returns:** `List<OrderReadModel>`

### Event Handlers

| Handler | Listens to | Action |
|---------|-----------|--------|
| `OnPaymentSucceededHandler` | `PaymentSucceeded` (Payment module) | Marks the order as paid via `MarkOrderPaidCommand` |
| `OnPaymentFailedHandler` | `PaymentFailed` (Payment module) | Optionally cancels the order or notifies support |

---

## REST Endpoints

### Customer-Facing

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/orders` | CUSTOMER | Create a new order |
| `GET` | `/api/v1/orders` | CUSTOMER | List own orders |
| `GET` | `/api/v1/orders/{id}` | CUSTOMER | Get own order details |
| `POST` | `/api/v1/orders/{id}/submit` | CUSTOMER | Submit a draft order |
| `POST` | `/api/v1/orders/{id}/cancel` | CUSTOMER | Cancel own order |

### Backoffice

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/admin/orders` | ADMIN, SUPPORT | List all orders |
| `GET` | `/api/v1/admin/orders/{id}` | ADMIN, SUPPORT | Get any order details |
| `POST` | `/api/v1/admin/orders/{id}/confirm` | ADMIN | Confirm a pending order |
| `POST` | `/api/v1/admin/orders/{id}/fulfill` | ADMIN | Fulfill a paid order |
| `POST` | `/api/v1/admin/orders/{id}/cancel` | ADMIN | Cancel any order |

### Request Bodies

**`POST /api/v1/orders`** — Create Order

| Field | Type | Validation |
|-------|------|------------|
| `items` | `List<OrderItemRequest>` | Required, non-empty |

`OrderItemRequest`:

| Field | Type | Validation |
|-------|------|------------|
| `productId` | `UUID` | Required |
| `quantity` | `Integer` | Required, min 1 |

**`POST /api/v1/orders/{id}/cancel`** — Cancel Order

| Field | Type | Validation |
|-------|------|------------|
| `reason` | `String` | Required, non-blank, max 500 characters |

### Response Body — `OrderResponse`

| Field | Type |
|-------|------|
| `id` | `UUID` |
| `customerId` | `UUID` |
| `status` | `String` |
| `totalAmount` | `BigDecimal` |
| `currency` | `String` |
| `lines` | `List<OrderLineResponse>` |
| `cancellationReason` | `String` |
| `createdAt` | `Instant` |
| `updatedAt` | `Instant` |

`OrderLineResponse`:

| Field | Type |
|-------|------|
| `id` | `UUID` |
| `productId` | `UUID` |
| `productName` | `String` |
| `quantity` | `int` |
| `unitPrice` | `BigDecimal` |
| `subtotal` | `BigDecimal` |

---

## Module Dependencies

| Direction | Module | Mechanism | Description |
|-----------|--------|-----------|-------------|
| **Depends on** | Product Catalog | `ProductQueryApi` (sync) | Retrieves product data when creating orders |
| **Depends on** | User (IAM) | User identity | Customer identity via `CustomerId` reference |
| **Provides to** | Payment | Domain events (async) | Payment reacts to `OrderCreated`, `OrderConfirmed` |
| **Reacts to** | Payment | Domain events (async) | Order reacts to `PaymentSucceeded`, `PaymentFailed` |
| **Provides to** | Notification | Domain events (async) | Notification reacts to order lifecycle events |
| **Provides to** | Subscription | Domain events (async) | Subscription reacts to `OrderFulfilled` |

---

## Business Rules

| Rule | Enforcement |
|------|-------------|
| An order must have at least one line | Aggregate constructor guard |
| Maximum 50 lines per order | `addLine()` method check |
| Quantity must be positive | Value object / guard clause |
| Only draft orders can be modified | Status check in `addLine()` |
| Only the owning customer can view or cancel their orders | Application layer + Spring Security |
| Fulfilled orders cannot be cancelled | Status transition check in `cancel()` |
| Product data is snapshotted at order creation time | `OrderLine` stores product name and price at creation |
| Order status transitions follow a strict state machine | Each transition method validates the current status |

---

## Related Documents

- [Bounded Contexts](../domain/bounded-contexts.md)
- [Module Structure](../architecture/module-structure.md)
- [Inter-Module Communication](../architecture/inter-module-communication.md)
- [Product Catalog Module](product-module.md)
- [Payment Module](payment-module.md)
