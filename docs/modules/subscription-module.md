# Subscription Management Module

## Purpose

This document describes the **Subscription Management** module of the CYNA Platform. This module manages the lifecycle of active cybersecurity service subscriptions — from activation through renewal, suspension, and cancellation.

---

## Overview

| Property | Value |
|----------|-------|
| Module name | `subscription` |
| Bounded context | Subscription Management |
| Base package | `com.cyna.modules.subscription` |
| Database schema | `subscription_schema` |
| Primary aggregate | `Subscription` |

The Subscription Management module represents the ongoing relationship between a customer and the services they have purchased. When an order is fulfilled, a subscription is activated. The module tracks billing cycles, handles automatic and manual renewals, and manages customer-initiated cancellations and expirations.

---

## Domain Model

### Aggregate: Subscription

| Field | Type | Description |
|-------|------|-------------|
| `id` | `SubscriptionId` | Unique subscription identifier |
| `customerId` | `CustomerId` | Reference to the customer |
| `productId` | `ProductId` | Reference to the subscribed product |
| `originOrderId` | `OrderId` | Reference to the originating order |
| `status` | `SubscriptionStatus` | Current state |
| `billingCycle` | `BillingCycle` | Renewal frequency |
| `recurringAmount` | `Money` | Recurring charge with currency |
| `startDate` | `Instant` | When the subscription started |
| `currentPeriodEnd` | `Instant` | End of the current billing period |
| `cancelledAt` | `Instant` | Cancellation timestamp (if cancelled) |
| `cancellationReason` | `String` | Free-text reason (if cancelled) |
| `autoRenew` | `boolean` | Whether auto-renewal is enabled (default: `true`) |
| `createdAt` | `Instant` | Creation timestamp |
| `updatedAt` | `Instant` | Last modification timestamp |

**Behaviors:**

| Method | Description |
|--------|-------------|
| `activate(customerId, productId, orderId, billingCycle, recurringAmount)` | Factory — creates an active subscription, raises `SubscriptionActivated` |
| `renew()` | Extends subscription by one billing cycle, raises `SubscriptionRenewed` |
| `cancel(reason)` | Cancels the subscription, raises `SubscriptionCancelled` |
| `expire()` | Expires an active subscription whose period has ended, raises `SubscriptionExpired` |
| `suspend()` | Suspends an active subscription, raises `SubscriptionSuspended` |
| `reactivate()` | Reactivates a suspended subscription, raises `SubscriptionReactivated` |
| `disableAutoRenew()` | Disables auto-renewal |

### Value Objects

| Value Object | Description | Validation Rules |
|-------------|-------------|------------------|
| `SubscriptionId` | Unique subscription identifier | Non-null UUID |
| `CustomerId` | Reference to the customer | Non-null UUID |
| `ProductId` | Reference to the subscribed product | Non-null UUID |
| `OrderId` | Reference to the originating order | Non-null UUID |
| `BillingCycle` | Renewal frequency | Enum: `MONTHLY` (1 month), `ANNUAL` (12 months) |
| `SubscriptionStatus` | Current state | Enum: `ACTIVE`, `SUSPENDED`, `CANCELLED`, `EXPIRED` |
| `Money` | Recurring amount with currency | Non-negative amount, non-blank currency |

### Subscription Status Transitions

```
ACTIVE → SUSPENDED → ACTIVE (reactivation)
ACTIVE → CANCELLED
ACTIVE → EXPIRED (period ended, auto-renew disabled)
SUSPENDED → CANCELLED
```

### Repository Interface

| Method | Description |
|--------|-------------|
| `save(Subscription)` | Persist a subscription |
| `findById(SubscriptionId)` | Find subscription by ID |
| `findByCustomerId(CustomerId)` | Find subscriptions for a customer |
| `findByStatus(SubscriptionStatus)` | Find subscriptions by status |
| `findExpiredActiveSubscriptions(Instant)` | Find active subscriptions past their period end |
| `findDueForRenewal(Instant)` | Find subscriptions due for renewal |

---

## Domain Events

| Event | Trigger | Payload |
|-------|---------|---------|
| `SubscriptionActivated` | Order fulfilled, subscription created | subscriptionId, customerId, productId, billingCycle, amount, currency, occurredAt |
| `SubscriptionRenewed` | Automatic or manual renewal | subscriptionId, nextPeriodEnd, occurredAt |
| `SubscriptionCancelled` | Customer or admin cancels | subscriptionId, reason, occurredAt |
| `SubscriptionExpired` | Period ended without renewal | subscriptionId, occurredAt |
| `SubscriptionSuspended` | Payment failure or admin action | subscriptionId, occurredAt |
| `SubscriptionReactivated` | Reactivation after suspension | subscriptionId, occurredAt |

---

## Application Layer

### Commands

#### `ActivateSubscriptionCommand` → `ActivateSubscriptionCommandHandler`

Creates and activates a new subscription. Typically triggered by `OrderFulfilled` event handler.

| Field | Type | Validation |
|-------|------|------------|
| `customerId` | `UUID` | Required |
| `productId` | `UUID` | Required |
| `orderId` | `UUID` | Required |
| `billingCycle` | `String` | Required, must match `BillingCycle` enum |
| `amount` | `BigDecimal` | Required, positive |
| `currency` | `String` | Required, non-blank |

**Returns:** `Result<SubscriptionId>`

#### `RenewSubscriptionCommand` → `RenewSubscriptionCommandHandler`

Renews an active subscription for one additional billing cycle.

| Field | Type | Validation |
|-------|------|------------|
| `subscriptionId` | `UUID` | Required, must be ACTIVE with auto-renew enabled |

**Returns:** `Result<Void>`

#### `CancelSubscriptionCommand` → `CancelSubscriptionCommandHandler`

Cancels a subscription.

| Field | Type | Validation |
|-------|------|------------|
| `subscriptionId` | `UUID` | Required, must not be CANCELLED or EXPIRED |
| `reason` | `String` | Required, non-blank, max 500 characters |

**Returns:** `Result<Void>`

#### `SuspendSubscriptionCommand` → `SuspendSubscriptionCommandHandler`

Suspends a subscription (e.g., due to payment failure).

| Field | Type | Validation |
|-------|------|------------|
| `subscriptionId` | `UUID` | Required, must be ACTIVE |

**Returns:** `Result<Void>`

#### `ReactivateSubscriptionCommand` → `ReactivateSubscriptionCommandHandler`

Reactivates a suspended subscription.

| Field | Type | Validation |
|-------|------|------------|
| `subscriptionId` | `UUID` | Required, must be SUSPENDED |

**Returns:** `Result<Void>`

#### `ProcessExpiredSubscriptionsCommand` → `ProcessExpiredSubscriptionsCommandHandler`

Batch job: finds overdue subscriptions with auto-renew disabled and expires them.

| Field | Type | Validation |
|-------|------|------------|
| `now` | `Instant` | Required, current timestamp |

**Returns:** `Result<Void>`

#### `ProcessRenewalsCommand` → `ProcessRenewalsCommandHandler`

Batch job: finds subscriptions due for renewal and processes them.

| Field | Type | Validation |
|-------|------|------------|
| `now` | `Instant` | Required, current timestamp |

**Returns:** `Result<Void>`

### Queries

#### `GetSubscriptionByIdQuery` → `GetSubscriptionByIdQueryHandler`

Retrieves a subscription by ID.

| Field | Type |
|-------|------|
| `subscriptionId` | `UUID` |

**Returns:** `SubscriptionReadModel` — id, customerId, productId, originOrderId, status, billingCycle, recurringAmount, currency, startDate, currentPeriodEnd, autoRenew, cancelledAt, cancellationReason, createdAt, updatedAt

#### `ListCustomerSubscriptionsQuery` → `ListCustomerSubscriptionsQueryHandler`

Lists subscriptions for a customer.

| Field | Type |
|-------|------|
| `customerId` | `UUID` |

**Returns:** `List<SubscriptionReadModel>`

#### `ListAllSubscriptionsQuery` → `ListAllSubscriptionsQueryHandler`

Lists all subscriptions (admin). No input fields.

**Returns:** `List<SubscriptionReadModel>`

### Event Handlers

| Handler | Listens to | Action |
|---------|-----------|--------|
| `OnOrderFulfilledHandler` | `OrderFulfilled` (Order module) | Activates subscriptions for the fulfilled order lines via `ActivateSubscriptionCommand` |
| `OnPaymentFailedHandler` | `PaymentFailed` (Payment module) | Suspends subscription when renewal payment fails via `SuspendSubscriptionCommand` |

---

## REST Endpoints

### Customer-Facing

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/subscriptions` | CUSTOMER | List own subscriptions |
| `GET` | `/api/v1/subscriptions/{id}` | CUSTOMER | Get own subscription details |
| `POST` | `/api/v1/subscriptions/{id}/cancel` | CUSTOMER | Cancel own subscription |
| `PUT` | `/api/v1/subscriptions/{id}/auto-renew` | CUSTOMER | Enable/disable auto-renewal |

### Backoffice

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/admin/subscriptions` | ADMIN, SUPPORT | List all subscriptions |
| `GET` | `/api/v1/admin/subscriptions/{id}` | ADMIN, SUPPORT | Get any subscription details |
| `POST` | `/api/v1/admin/subscriptions/{id}/suspend` | ADMIN | Suspend a subscription |
| `POST` | `/api/v1/admin/subscriptions/{id}/reactivate` | ADMIN | Reactivate a suspended subscription |
| `POST` | `/api/v1/admin/subscriptions/{id}/cancel` | ADMIN | Cancel any subscription |

### Request Bodies

**`POST /api/v1/subscriptions/{id}/cancel`** — Cancel Subscription

| Field | Type | Validation |
|-------|------|------------|
| `reason` | `String` | Required, non-blank, max 500 characters |

**`PUT /api/v1/subscriptions/{id}/auto-renew`** — Toggle Auto-Renewal

| Field | Type | Validation |
|-------|------|------------|
| `autoRenew` | `Boolean` | Required |

### Response Body — `SubscriptionResponse`

| Field | Type |
|-------|------|
| `id` | `UUID` |
| `customerId` | `UUID` |
| `productId` | `UUID` |
| `status` | `String` |
| `billingCycle` | `String` |
| `recurringAmount` | `BigDecimal` |
| `currency` | `String` |
| `startDate` | `Instant` |
| `currentPeriodEnd` | `Instant` |
| `autoRenew` | `boolean` |
| `cancelledAt` | `Instant` |
| `cancellationReason` | `String` |
| `createdAt` | `Instant` |
| `updatedAt` | `Instant` |

---

## Scheduled Jobs

| Job | Schedule | Command triggered | Description |
|-----|----------|-------------------|-------------|
| Renewal processing | Daily at 2:00 AM | `ProcessRenewalsCommand` | Finds subscriptions due for renewal and processes them |
| Expiration processing | Daily at 3:00 AM | `ProcessExpiredSubscriptionsCommand` | Finds overdue subscriptions with auto-renew disabled and expires them |
| Payment failure handling | On event | `SuspendSubscriptionCommand` | Suspends subscriptions when renewal payment fails |

---

## Module Dependencies

| Direction | Module | Mechanism | Description |
|-----------|--------|-----------|-------------|
| **Reacts to** | Order | Domain events (async) | Activates subscriptions when `OrderFulfilled` is received |
| **Reacts to** | Payment | Domain events (async) | Suspends subscription on `PaymentFailed` for renewals |
| **Provides to** | Notification | Domain events (async) | Notification sends renewal reminders, cancellation confirmations |
| **Depends on** | User (IAM) | User identity | Customer identity for authorization |

---

## Business Rules

| Rule | Enforcement |
|------|-------------|
| Only active subscriptions can be renewed | Status check in `renew()` |
| Only active subscriptions can be suspended | Status check in `suspend()` |
| Cancelled and expired subscriptions are final states | Status transition guards |
| Auto-renewal can be toggled by the customer | `disableAutoRenew()` method |
| Cancellation takes effect at the end of the current period | Business logic in cancellation flow |
| Suspension stops service access immediately | Status used for service access checks |
| Renewal failure suspends the subscription | Event handler for `PaymentFailed` |
| Customers can only manage their own subscriptions | Application layer + Spring Security |

---

## Related Documents

- [Bounded Contexts](../domain/bounded-contexts.md)
- [Module Structure](../architecture/module-structure.md)
- [Inter-Module Communication](../architecture/inter-module-communication.md)
- [Order Module](order-module.md)
- [Payment Module](payment-module.md)
