# Bounded Contexts

## Purpose

This document defines the bounded contexts of the CYNA Platform, their responsibilities, and their relationships. Bounded contexts are the foundation of the system's strategic design, mapping business capabilities to software modules.

---

## What Is a Bounded Context?

A **bounded context** is a semantic boundary within which a particular domain model is defined and applicable. Each bounded context:

- Has its own **ubiquitous language** — the same word may mean different things in different contexts.
- Owns its **data** — no shared database tables across contexts.
- Maps to a **module** in the Modular Monolith.
- Is a candidate for extraction into a **microservice**.

---

## CYNA Platform Bounded Contexts

### 1. Identity & Access Management (IAM)

**Module**: `user`

**Responsibility**: Manages user identities, authentication, and authorization.

| Concept | Description |
|---------|-------------|
| User | A registered person (customer or admin) |
| Credentials | Email + hashed password |
| Role | `CUSTOMER`, `ADMIN`, `SUPPORT` |
| Session | JWT access token + refresh token pair |
| Profile | User's personal information |

**Key Aggregates**: `User`

**Domain Events**:
- `UserRegistered`
- `UserActivated`
- `UserDeactivated`
- `PasswordChanged`
- `RoleAssigned`

**Ubiquitous Language**:
- "User" = authenticated identity with roles
- "Registration" = process of creating a new identity
- "Activation" = enabling a previously inactive user

---

### 2. Product Catalog

**Module**: `product`

**Responsibility**: Manages the catalog of cybersecurity services and their pricing.

| Concept | Description |
|---------|-------------|
| Product | A cybersecurity service offering (SOC, EDR, XDR) |
| Category | Classification of products (e.g., endpoint, network, cloud) |
| Pricing | Price, billing cycle, currency |
| Feature | A capability included in a product |
| Availability | Whether a product is published and purchasable |

**Key Aggregates**: `Product`

**Domain Events**:
- `ProductCreated`
- `ProductUpdated`
- `ProductPublished`
- `ProductUnpublished`
- `PriceChanged`

**Ubiquitous Language**:
- "Product" = a purchasable cybersecurity service
- "Publishing" = making a product visible and purchasable
- "Feature" = a technical capability included in the service

---

### 3. Order Management

**Module**: `order`

**Responsibility**: Manages the lifecycle of customer orders from creation through fulfillment.

| Concept | Description |
|---------|-------------|
| Order | A customer's intent to purchase one or more products |
| Order Line | A single product within an order |
| Order Status | PENDING → CONFIRMED → PAID → FULFILLED → CANCELLED |
| Total | Calculated sum of all order lines |

**Key Aggregates**: `Order`

**Domain Events**:
- `OrderCreated`
- `OrderConfirmed`
- `OrderPaid`
- `OrderFulfilled`
- `OrderCancelled`

**Ubiquitous Language**:
- "Order" = a customer's purchase transaction
- "Confirmation" = validating that the order can be processed
- "Fulfillment" = activating the subscribed services

---

### 4. Payment

**Module**: `payment`

**Responsibility**: Handles payment processing, integrating with external payment providers.

| Concept | Description |
|---------|-------------|
| Payment | A monetary transaction associated with an order |
| Payment Method | The instrument used (credit card, bank transfer) |
| Payment Status | PENDING → SUCCEEDED → FAILED → REFUNDED |
| Invoice | A financial document generated after successful payment |
| Refund | A reversal of a previous payment |

**Key Aggregates**: `Payment`

**Domain Events**:
- `PaymentInitiated`
- `PaymentSucceeded`
- `PaymentFailed`
- `RefundRequested`
- `RefundCompleted`
- `InvoiceGenerated`

**Ubiquitous Language**:
- "Payment" = a single attempt to collect money for an order
- "Refund" = returning money to the customer
- "Invoice" = financial proof of payment

---

### 5. Subscription Management

**Module**: `subscription`

**Responsibility**: Manages active service subscriptions, renewals, and lifecycle.

| Concept | Description |
|---------|-------------|
| Subscription | An active, time-bound service engagement |
| Billing Cycle | Monthly, quarterly, annual |
| Renewal | Automatic or manual continuation of a subscription |
| Cancellation | Customer-initiated termination |

**Key Aggregates**: `Subscription`

**Domain Events**:
- `SubscriptionActivated`
- `SubscriptionRenewed`
- `SubscriptionCancelled`
- `SubscriptionExpired`

---

### 6. Notification

**Module**: `notification`

**Responsibility**: Sends notifications to users via email, SMS, or in-app messages.

| Concept | Description |
|---------|-------------|
| Notification | A message sent to a user |
| Channel | Email, SMS, push notification |
| Template | A reusable notification format |

**Key Aggregates**: `Notification` (or just uses application services without a rich domain model)

**Domain Events** (consumes from other modules):
- Reacts to `UserRegistered` → send welcome email
- Reacts to `OrderConfirmed` → send order confirmation
- Reacts to `PaymentSucceeded` → send payment receipt

---

## Context Map

The context map shows relationships between bounded contexts:

```
┌─────────────┐         ┌──────────────┐
│    IAM      │         │   Product    │
│   (User)    │         │   Catalog    │
└──────┬──────┘         └──────┬───────┘
       │                       │
       │  User identity        │  Product info
       │  used by all          │  used by Order
       │  contexts             │
       ▼                       ▼
┌──────────────────────────────────────┐
│           Order Management           │
│                                      │
│  Needs: User identity (from IAM)     │
│  Needs: Product info (from Catalog)  │
│  Publishes: OrderPaid (to Payment)   │
└──────────────────┬───────────────────┘
                   │
                   │  OrderCreated / OrderConfirmed
                   ▼
          ┌────────────────┐
          │    Payment     │
          │                │
          │ Reacts to Order│
          │ events         │
          └────────┬───────┘
                   │
                   │  PaymentSucceeded
                   ▼
          ┌────────────────┐
          │  Notification  │
          │                │
          │ Reacts to all  │
          │ domain events  │
          └────────────────┘
```

---

## Relationship Types

| Relationship | Between | Type | Description |
|-------------|---------|------|-------------|
| IAM → Order | IAM, Order | Shared Kernel (User ID) | Order references user by ID |
| Catalog → Order | Product, Order | Customer/Supplier | Order consumes product data via API |
| Order → Payment | Order, Payment | Published Language (Events) | Payment reacts to order events |
| Payment → Order | Payment, Order | Published Language (Events) | Order reacts to payment events |
| * → Notification | All, Notification | Published Language (Events) | Notification reacts to all events |
| Payment → External PSP | Payment, Stripe/etc. | Anti-Corruption Layer | ACL protects domain from external API |

---

## Anti-Corruption Layers

When integrating with external systems, an **Anti-Corruption Layer (ACL)** translates external models into internal domain models. This prevents external API changes from corrupting the domain.

**Example: Payment Provider Integration**

```java
// External Stripe response (raw)
StripePaymentIntent stripeResponse = stripeClient.createPaymentIntent(...);

// ACL translates to domain
Payment payment = PaymentAclMapper.toDomain(stripeResponse);
```

The ACL lives in the `infrastructure.adapter` package and translates:
- External DTOs → Domain value objects
- External status codes → Domain enums
- External error formats → Domain-compatible errors

---

## Rules for Defining Bounded Contexts

1. **One context = one module.** Do not merge multiple contexts into a single module.
2. **Same word, different meaning?** You likely have two contexts. Example: "Product" in the catalog is different from "OrderLine" in an order.
3. **Keep contexts autonomous.** A context should be able to operate with minimal dependencies on others.
4. **Prefer events over queries** for cross-context communication when eventual consistency is acceptable.
5. **Start coarse-grained.** It is easier to split a context later than to merge two.

---

## Common Mistakes

| Mistake | Consequence | Fix |
|---------|-------------|-----|
| Creating too many small contexts | Excessive inter-module communication overhead | Merge related concepts into one context |
| Sharing domain entities across contexts | Tight coupling, changes cascade | Each context defines its own model |
| Using the same database table from two contexts | Data ownership violation | Each context owns its tables |
| Ignoring ubiquitous language differences | Confusion, ambiguity in code | Explicitly name concepts per context |

---

## Related Documents

- [Architecture Overview](../architecture/architecture-overview.md)
- [Aggregates](aggregates.md)
- [Domain Events](domain-events.md)
- [Inter-Module Communication](../architecture/inter-module-communication.md)
