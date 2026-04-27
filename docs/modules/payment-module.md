# Payment Module

## Purpose

This document describes the **Payment** module of the CYNA Platform. This module handles payment processing, integration with external payment service providers (PSP), refund management, and invoice generation.

---

## Overview

| Property | Value |
|----------|-------|
| Module name | `payment` |
| Bounded context | Payment Processing |
| Base package | `com.cyna.modules.payment` |
| Database schema | `payment_schema` |
| Primary aggregate | `Payment` |

The Payment module is responsible for collecting money from customers for their orders. It integrates with external payment providers (e.g., Stripe) through an **Anti-Corruption Layer (ACL)**, ensuring the domain model is not contaminated by external API structures. The module reacts to order events and publishes payment outcomes consumed by the Order and Notification modules.

---

## Domain Model

### Aggregate: Payment

| Field | Type | Description |
|-------|------|-------------|
| `id` | `PaymentId` | Unique payment identifier |
| `orderId` | `OrderId` | Reference to the associated order |
| `customerId` | `CustomerId` | Reference to the customer |
| `amount` | `Money` | Payment amount with currency |
| `paymentMethod` | `PaymentMethod` | Payment instrument type |
| `status` | `PaymentStatus` | Current state of the payment |
| `failureReason` | `String` | Failure detail (when failed) |
| `externalTransactionId` | `String` | PSP transaction reference |
| `createdAt` | `Instant` | Creation timestamp |
| `updatedAt` | `Instant` | Last modification timestamp |

**Behaviors:**

| Method | Description |
|--------|-------------|
| `create(orderId, customerId, amount, paymentMethod)` | Factory method — creates a pending payment, raises `PaymentInitiated` |
| `markSucceeded(externalTransactionId)` | Marks payment as succeeded, raises `PaymentSucceeded` |
| `markFailed(reason)` | Marks payment as failed, raises `PaymentFailed` |
| `requestRefund()` | Initiates a refund for a succeeded payment, raises `RefundRequested` |
| `completeRefund()` | Completes the refund, raises `RefundCompleted` |

### Value Objects

| Value Object | Description | Validation Rules |
|-------------|-------------|------------------|
| `PaymentId` | Unique payment identifier | Non-null UUID |
| `OrderId` | Reference to the associated order | Non-null UUID |
| `CustomerId` | Reference to the customer | Non-null UUID |
| `Money` | Payment amount with currency | Non-negative amount, non-blank currency |
| `PaymentMethod` | Payment instrument type | Enum: `CREDIT_CARD`, `BANK_TRANSFER` |
| `PaymentStatus` | Current state of the payment | Enum with valid transitions |

### Payment Status Transitions

```
PENDING → SUCCEEDED → REFUND_REQUESTED → REFUNDED
    ↓
  FAILED
```

### Port Interface — `PaymentGatewayPort`

| Method | Description |
|--------|-------------|
| `initiatePayment(PaymentId, Money, PaymentMethod)` | Initiates payment with external PSP, returns `PaymentGatewayResult` |
| `initiateRefund(PaymentId, externalTransactionId, Money)` | Initiates refund with external PSP, returns `PaymentGatewayResult` |

`PaymentGatewayResult`: `isSuccessful`, `transactionId`, `errorMessage`

### Repository Interface

| Method | Description |
|--------|-------------|
| `save(Payment)` | Persist a payment |
| `findById(PaymentId)` | Find payment by ID |
| `findByOrderId(OrderId)` | Find payment for a specific order |
| `findByCustomerId(CustomerId)` | Find payments for a customer |
| `findAll()` | List all payments |

---

## Domain Events

| Event | Trigger | Payload |
|-------|---------|---------|
| `PaymentInitiated` | Payment process starts | paymentId, orderId, customerId, amount, currency, paymentMethod, occurredAt |
| `PaymentSucceeded` | PSP confirms payment | paymentId, orderId, amount, currency, paymentMethod, occurredAt |
| `PaymentFailed` | PSP rejects payment | paymentId, orderId, reason, occurredAt |
| `RefundRequested` | Refund initiated | paymentId, orderId, amount, currency, occurredAt |
| `RefundCompleted` | Refund processed | paymentId, orderId, amount, currency, occurredAt |
| `InvoiceGenerated` | Invoice created after payment | invoiceId, paymentId, orderId, occurredAt |

---

## Application Layer

### Commands

#### `InitiatePaymentCommand` → `InitiatePaymentCommandHandler`

Creates a payment and initiates processing with PSP via `PaymentGatewayPort`.

| Field | Type | Validation |
|-------|------|------------|
| `orderId` | `UUID` | Required |
| `customerId` | `UUID` | Required |
| `amount` | `BigDecimal` | Required, positive |
| `currency` | `String` | Required, non-blank |
| `paymentMethod` | `String` | Required, must match `PaymentMethod` enum |

**Returns:** `Result<PaymentId>`

**Flow:** Create Payment → save → call `PaymentGatewayPort.initiatePayment()` → mark succeeded or failed based on result → save again → publish events.

#### `ProcessPaymentResultCommand` → `ProcessPaymentResultCommandHandler`

Handles PSP callback result (webhook). Must be idempotent.

| Field | Type | Validation |
|-------|------|------------|
| `externalTransactionId` | `String` | Required |
| `status` | `String` | Required (`SUCCEEDED` or `FAILED`) |
| `failureReason` | `String` | Optional (required if FAILED) |

**Returns:** `Result<Void>`

#### `RequestRefundCommand` → `RequestRefundCommandHandler`

Initiates a refund for a succeeded payment. Calls `PaymentGatewayPort.initiateRefund()`.

| Field | Type | Validation |
|-------|------|------------|
| `paymentId` | `UUID` | Required, must be a SUCCEEDED payment |

**Returns:** `Result<Void>`

#### `CompleteRefundCommand` → `CompleteRefundCommandHandler`

Marks a refund as completed (after PSP confirmation).

| Field | Type | Validation |
|-------|------|------------|
| `paymentId` | `UUID` | Required, must be REFUND_REQUESTED |

**Returns:** `Result<Void>`

### Queries

#### `GetPaymentByIdQuery` → `GetPaymentByIdQueryHandler`

Retrieves a single payment by ID.

| Field | Type |
|-------|------|
| `paymentId` | `UUID` |

**Returns:** `PaymentReadModel` — id, orderId, customerId, amount, currency, paymentMethod, status, failureReason, createdAt, updatedAt

#### `GetPaymentByOrderIdQuery` → `GetPaymentByOrderIdQueryHandler`

Retrieves payment for a specific order.

| Field | Type |
|-------|------|
| `orderId` | `UUID` |

**Returns:** `PaymentReadModel`

#### `ListCustomerPaymentsQuery` → `ListCustomerPaymentsQueryHandler`

Lists all payments for a customer.

| Field | Type |
|-------|------|
| `customerId` | `UUID` |

**Returns:** `List<PaymentReadModel>`

### Event Handlers

| Handler | Listens to | Action |
|---------|-----------|--------|
| `OnOrderConfirmedHandler` | `OrderConfirmed` (Order module) | Triggers payment initiation via `InitiatePaymentCommand` |

---

## Anti-Corruption Layer (ACL)

The Payment module integrates with external payment providers through an ACL. The adapter translates external API models into domain-compatible structures.

### ACL Translation Flow

```
External World                    ACL (Infrastructure)              Domain
┌──────────────┐                 ┌─────────────────────┐         ┌──────────────┐
│ Stripe API   │────response────▶│ StripePaymentAdapter │────────▶│ Payment      │
│              │                 │                     │  Domain  │ Aggregate    │
│ PaymentIntent│                 │ Translates:         │  types   │              │
│ { id, status │                 │ - Stripe ID → txnId │         │              │
│   amount }   │                 │ - status → result   │         │              │
└──────────────┘                 └─────────────────────┘         └──────────────┘
```

**Key ACL rules:**

- External DTOs never cross the infrastructure boundary.
- The adapter maps external error codes to domain-compatible messages.
- The adapter can be swapped for a different PSP without any domain changes.

### Stripe Adapter Implementation

`StripePaymentAdapter` implements `PaymentGatewayPort`:
- `initiatePayment()` → maps to `stripeClient.createPaymentIntent()` (amount in cents, lowercase currency)
- `initiateRefund()` → maps to `stripeClient.createRefund()`
- Payment method mapping: `CREDIT_CARD` → `"card"`, `BANK_TRANSFER` → `"sepa_debit"`

---

## REST Endpoints

### Customer-Facing

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/payments` | CUSTOMER | Initiate a payment for an order |
| `GET` | `/api/v1/payments/{id}` | CUSTOMER | Get own payment details |
| `GET` | `/api/v1/payments/order/{orderId}` | CUSTOMER | Get payment for a specific order |

### Webhook

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/payments/webhook` | Public (PSP) | PSP webhook callback |

**Webhook requirements:**
- **Idempotent** — processing the same webhook twice must not cause duplicate state changes
- **Signature-verified** — payload signature validated against PSP signing secret
- **Immediate acknowledgment** — return 200 OK immediately, process asynchronously if needed

### Backoffice

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/admin/payments` | ADMIN, SUPPORT | List all payments |
| `GET` | `/api/v1/admin/payments/{id}` | ADMIN, SUPPORT | Get any payment details |
| `POST` | `/api/v1/admin/payments/{id}/refund` | ADMIN | Initiate a refund |

### Request Bodies

**`POST /api/v1/payments`** — Initiate Payment

| Field | Type | Validation |
|-------|------|------------|
| `orderId` | `UUID` | Required |
| `paymentMethod` | `String` | Required, non-blank |

**`POST /api/v1/admin/payments/{id}/refund`** — Request Refund

| Field | Type | Validation |
|-------|------|------------|
| `reason` | `String` | Optional, max 500 characters |

### Response Body — `PaymentResponse`

| Field | Type |
|-------|------|
| `id` | `UUID` |
| `orderId` | `UUID` |
| `customerId` | `UUID` |
| `amount` | `BigDecimal` |
| `currency` | `String` |
| `paymentMethod` | `String` |
| `status` | `String` |
| `failureReason` | `String` |
| `createdAt` | `Instant` |
| `updatedAt` | `Instant` |

---

## Module Dependencies

| Direction | Module | Mechanism | Description |
|-----------|--------|-----------|-------------|
| **Reacts to** | Order | Domain events (async) | Reacts to `OrderConfirmed` to initiate payment |
| **Provides to** | Order | Domain events (async) | Publishes `PaymentSucceeded` / `PaymentFailed` |
| **Provides to** | Notification | Domain events (async) | Notification sends payment receipts and failure alerts |
| **Depends on** | User (IAM) | User identity | Customer identity for authorization |
| **Depends on** | External PSP (Stripe) | ACL adapter | Payment processing via `PaymentGatewayPort` |

---

## Business Rules

| Rule | Enforcement |
|------|-------------|
| One payment per order (at a time) | Application layer check |
| Payment amount must match the order total | Application layer validation |
| Only succeeded payments can be refunded | Status check in `requestRefund()` |
| Webhook processing must be idempotent | Idempotency check on `externalTransactionId` |
| PSP webhook signatures must be verified | Infrastructure layer (Stripe adapter) |
| Refund amount cannot exceed the original payment | Application layer validation |
| Payment failure must not block order cancellation | Order module handles cancellation independently |
| Sensitive payment data (card numbers) must never be stored | PSP handles card data; only references are stored |

---

## Security Considerations

| Concern | Mitigation |
|---------|------------|
| PCI DSS compliance | Card data handled entirely by the PSP (Stripe); never touches our servers |
| Webhook spoofing | Signature verification on all webhook payloads |
| Replay attacks | Idempotency keys and `externalTransactionId` uniqueness |
| Unauthorized refunds | ADMIN-only access to refund endpoints |
| Data exposure | Payment responses never include full card numbers or CVV |

---

## Related Documents

- [Bounded Contexts](../domain/bounded-contexts.md)
- [Module Structure](../architecture/module-structure.md)
- [Inter-Module Communication](../architecture/inter-module-communication.md)
- [Order Module](order-module.md)
- [Authentication](../security/authentication.md)
