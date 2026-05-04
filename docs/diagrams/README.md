# Diagrams

## Purpose

This folder contains architecture and design diagrams for the CYNA Platform. Diagrams are maintained alongside code and should be updated when architectural decisions change.

---

## Diagram Index

| Diagram | Description | Format | Source |
|---------|-------------|--------|--------|
| System Context | High-level system overview showing actors and external systems | Mermaid | this README |
| Module Dependency Map | Shows allowed dependencies between backend modules | Mermaid | this README |
| Clean Architecture Layers | Concentric layer diagram with dependency arrows | Mermaid / PNG | TBD |
| Authentication Flow | JWT login, refresh, and token rotation sequence | Mermaid | this README |
| Order Lifecycle | State machine for order status transitions | Mermaid | [order-payment-flow](../flows/order-payment-flow.md) |
| Domain Event Flow | Event publishing and handling across modules | Mermaid / PNG | TBD |
| Deployment Architecture | Infrastructure and deployment topology | Mermaid / PNG | TBD |
| **Orders & Payments — module map** | Component diagram: Order/Payment/Subscription modules + Stripe | Mermaid | [order-payment-flow §2](../flows/order-payment-flow.md#2-architecture-des-modules-concernés) |
| **Orders & Payments — data model** | ER diagram: orders, payments, subscriptions, stripe_customers, stripe_products | Mermaid | [order-payment-flow §3](../flows/order-payment-flow.md#3-modèle-de-données) |
| **Purchase happy path** | Sequence: order → Stripe Subscription → webhook → activation | Mermaid | [order-payment-flow §5](../flows/order-payment-flow.md#5-flux--achat-initial-happy-path) |
| **Subscription renewal** | Sequence: Stripe auto-charge → invoice.paid → period extension | Mermaid | [order-payment-flow §6](../flows/order-payment-flow.md#6-flux--renouvellement-automatique) |
| **Subscription cancellation** | Sequence: UI cancel → Stripe cancel → local update + webhook | Mermaid | [order-payment-flow §7](../flows/order-payment-flow.md#7-flux--annulation-par-le-client) |
| **Payment failed (past_due)** | Sequence: dunning, smart retries, recovery or final cancel | Mermaid | [order-payment-flow §8](../flows/order-payment-flow.md#8-flux--échec-de-paiement-past_due--dunning) |
| **3D Secure (SCA)** | Sequence: confirmCardPayment → 3DS challenge → success/failure | Mermaid | [order-payment-flow §9](../flows/order-payment-flow.md#9-flux--authentification-3d-secure-sca) |
| **Webhook routing** | Flowchart: dispatch by event type and billing_reason | Mermaid | [order-payment-flow §10](../flows/order-payment-flow.md#10-routage-des-webhooks-stripe) |
| **Order / Payment / Subscription state machines** | Three concise state diagrams covering all transitions | Mermaid | [order-payment-flow §11](../flows/order-payment-flow.md#11-machines-à-états) |
| **Stripe entity map** | What Cyna entities map to which Stripe resources | Mermaid | [order-payment-flow §13](../flows/order-payment-flow.md#13-entités-stripe-créées-et-idempotence) |

---

## Diagramming Tools

| Tool | Purpose | Format |
|------|---------|--------|
| **Mermaid** (preferred) | Inline diagrams in Markdown | `.md` / embedded |
| **Draw.io / diagrams.net** | Complex visual diagrams | `.drawio` / `.png` |
| **PlantUML** | UML diagrams (class, sequence) | `.puml` / `.png` |

### Mermaid Examples

#### System Context

```mermaid
graph TB
    Customer[Customer<br/>Browser/PWA]
    Admin[Administrator<br/>Backoffice]
    
    Customer -->|HTTPS| API[CYNA Backend API<br/>Spring Boot]
    Admin -->|HTTPS| API
    
    API -->|JDBC| DB[(PostgreSQL)]
    API -->|HTTPS| PSP[Payment Provider<br/>Stripe]
    API -->|SMTP| Email[Email Service]
```

#### Order State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING: Order created
    PENDING --> CONFIRMED: Order confirmed
    CONFIRMED --> PAID: Payment succeeded
    PAID --> FULFILLED: Services activated
    PENDING --> CANCELLED: Customer cancels
    CONFIRMED --> CANCELLED: Admin cancels
    FULFILLED --> [*]
    CANCELLED --> [*]
```

#### Authentication Sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant A as Auth API
    participant DB as Database
    
    C->>A: POST /auth/login {email, password}
    A->>DB: Find user by email
    DB-->>A: User record
    A->>A: Verify password (bcrypt)
    A->>A: Generate JWT access token
    A->>DB: Store refresh token (hashed)
    A-->>C: {accessToken, refreshToken}
    
    Note over C,A: Later, access token expires...
    
    C->>A: POST /auth/refresh {refreshToken}
    A->>DB: Find refresh token (by hash)
    A->>A: Validate not expired/revoked
    A->>DB: Revoke old, store new refresh token
    A->>A: Generate new JWT access token
    A-->>C: {newAccessToken, newRefreshToken}
```

#### Module Dependencies

```mermaid
graph LR
    Order[Order Module]
    Product[Product Module]
    User[User Module]
    Payment[Payment Module]
    Notification[Notification Module]
    
    Order -->|Public API| Product
    Order -->|Public API| User
    Payment -.->|Domain Event| Order
    Order -.->|Domain Event| Payment
    Order -.->|Domain Event| Notification
    Payment -.->|Domain Event| Notification
    User -.->|Domain Event| Notification
```

---

## Conventions

| Convention | Rule |
|-----------|------|
| Prefer Mermaid for simple diagrams | Embedded in Markdown, version-controllable |
| Use Draw.io for complex diagrams | Export as PNG and commit both `.drawio` and `.png` |
| Name diagram files descriptively | `system-context.md`, `order-state-machine.drawio` |
| Keep diagrams up to date | Update when architecture changes |
| Include diagrams in relevant docs | Reference from architecture documents |
| Use consistent colors and shapes | Follow team conventions |

---

## Creating New Diagrams

1. Choose the appropriate tool (Mermaid for simple, Draw.io for complex).
2. Create the diagram file in this folder.
3. Update this README with the new diagram entry.
4. Reference the diagram from the relevant documentation page.
5. Include the diagram in your PR for review.

---

## Related Documents

- [Architecture Overview](../architecture/architecture-overview.md)
- [Bounded Contexts](../domain/bounded-contexts.md)
- [Authentication](../security/authentication.md)
- [Inter-Module Communication](../architecture/inter-module-communication.md)
- [**Orders & Payments — flux complet**](../flows/order-payment-flow.md) — toutes les séquences + diagrammes d'état Stripe
