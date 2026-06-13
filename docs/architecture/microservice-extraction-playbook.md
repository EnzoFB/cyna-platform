# Microservice Extraction Playbook

This codebase is a **modular monolith built to be split-ready**: extracting any
module into its own service should be a mechanical, low-risk operation rather
than a rewrite. This document records *why* that is true and *exactly what to do*
when the day comes.

## The extraction seam is already in the code

Modules never touch each other directly. All cross-module interaction goes
through two — and only two — channels, both enforced by tests:

1. **Synchronous → `application.api` interfaces.** A module calls another only
   through its published API (`UserQueryApi`, `UserCommandApi`, `OrderQueryApi`,
   `OrderCommandApi`, `PaymentQueryApi`, `PaymentCommandApi`,
   `SubscriptionQueryApi`, `SubscriptionCommandApi`, `ProductQueryApi`). The
   implementations are package-private `@Service` beans.
2. **Asynchronous → domain events.** A module reacts to another's published
   `domain.event` via a Spring listener in its own `infrastructure/event/`.

These are the future network boundaries. At extraction:
- each `*QueryApi` / `*CommandApi` implementation is swapped from an in-process
  bean to an **HTTP/gRPC client** — callers do not change (they depend on the
  interface);
- domain-event listeners move behind a **message broker** (the publisher already
  emits events through `DomainEventPublisher`).

Enforced by `LayerDependencyRulesTest` + `ModuleIsolationRulesTest` (16 rules):
no cross-module access outside `application.api` / `domain.event`, and the module
graph is **acyclic** (a cycle could not be split).

## Module ownership

| Module | Owns schema | Notes |
|---|---|---|
| `user` | `user_schema` | identity, auth, addresses, consents |
| `product` | `product_schema` | catalog, promotions, carousel |
| `cart` | `cart_schema` | |
| `order` | `order_schema` | legally-retained accounting records |
| `subscription` | `subscription_schema` | |
| `payment` | `payment_schema` | Stripe integration |
| `dashboard` | `dashboard_schema` | admin reporting; KPIs computed via the other modules' QueryApis (no cross-schema read) |
| `account` | — (none) | pure orchestrator (RGPD export/erasure); only depends on other modules' APIs |

Each module reads **only its own schema**. This is enforced by
`SchemaIsolationRulesTest`, which fails if any module's code references another
module's `*_schema.`.

## What is NOT yet split — and the one step to finish it

The single remaining database-level coupling is a set of **8 cross-schema
foreign-key constraints**, kept deliberately as an integrity safety net while we
run as one database. They do **not** block extraction because the code never
relies on them for reads (all cross-module reads go through APIs) and write-side
references are validated through the owning module's API. At extraction they are
removed by a single migration — see
[`extraction-drop-cross-schema-fks.sql`](extraction-drop-cross-schema-fks.sql).

| Constraint | Table | References |
|---|---|---|
| `fk_subscriptions_user` | `subscription_schema.subscriptions` | `user_schema.users` |
| `fk_subscriptions_order` | `subscription_schema.subscriptions` | `order_schema.orders` |
| `fk_subscriptions_product` | `subscription_schema.subscriptions` | `product_schema.products` |
| `fk_stripe_customers_user` | `payment_schema.stripe_customers` | `user_schema.users` |
| `fk_stripe_products_product` | `payment_schema.stripe_products` | `product_schema.products` |
| `fk_payments_order` | `payment_schema.payments` | `order_schema.orders` |
| `fk_payments_user` | `payment_schema.payments` | `user_schema.users` |
| `payment_consent_log_user_id_fkey` | `payment_schema.payment_consent_log` | `user_schema.users` |

After these are dropped, the referential integrity they provided is upheld by
the application: the referencing module validates the foreign id through the
owning module's API on write, and reacts to deletions/anonymization through
domain events (e.g. the `account` module orchestrates RGPD erasure across
modules; payment reacts to subscription renewal-preference events).

## Extraction checklist (per module)

1. **Database** — move the module's schema to its own database instance; run
   `extraction-drop-cross-schema-fks.sql` to remove the cross-schema FKs that
   reference (or are referenced by) the extracted module.
2. **Synchronous calls** — replace the in-process `*QueryApi`/`*CommandApi`
   implementation it consumes with a remote client (same interface). Add
   resilience (timeouts, retries, circuit breaker) at this seam.
3. **Asynchronous events** — point `DomainEventPublisher` at the message broker;
   re-subscribe the module's `infrastructure/event/` listeners to the broker
   topics instead of the in-VM Spring event bus.
4. **Config/security** — give the service its own datasource + JWT validation;
   the HTTP contracts (`/api/v1/...`) are unchanged.
5. **Guardrails** — the architecture tests (`com.cyna.architecture.*`) continue
   to run per remaining module and keep the boundaries honest.

## What keeps this honest day-to-day

- `LayerDependencyRulesTest` — clean layering (domain pure, etc.).
- `ModuleIsolationRulesTest` — cross-module only via `application.api` /
  `domain.event`, and no module cycles.
- `SchemaIsolationRulesTest` — each module reads only its own schema.

Run them all with: `./gradlew test --tests 'com.cyna.architecture.*'`.
