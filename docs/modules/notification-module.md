# Notification Module

> **Status: implemented** (branch `spike/notification-module`).
> This document describes the module **as built**. An earlier version of this
> file specified an aspirational design (a persisted `Notification` aggregate,
> `notification_schema`, retry queue, multi-channel SMS/push, REST history
> endpoints) that was never implemented — see [Known limitations](#known-limitations)
> for what intentionally differs.

## Purpose

The **notification** module owns every transactional message the platform
sends (currently email, via Brevo). It is the single place that knows *what*
messages exist and *how* they are rendered and delivered.

It is a **coordination module** in the spirit of `account`: it owns **no
domain model and no Postgres schema**. Its job is to react to what happens in
the other modules and turn it into a message.

| Property | Value |
|----------|-------|
| Base package | `com.cyna.modules.notification` |
| Database schema | **none** (owns no data) |
| Layers present | `application`, `infrastructure`, `interfaces` (no `domain`) |
| Delivery | Brevo HTTP API + Thymeleaf templates + Spring `MessageSource` (i18n fr/en) |

## Dependency direction (why it looks the way it does)

The defining constraint is **one-way dependencies**, enforced by
`ModuleIsolationRulesTest` (no module cycles — the codebase must stay
split-ready):

```
notification  ───►  user / order / subscription
   (depends on their domain.event + application.api)

nobody depends on notification
```

If any module called the notification module *and* notification reacted to that
module's events, ArchUnit would fail on a module cycle. So **every** trigger is
an inbound **domain event** the module subscribes to. No other module imports
anything from `notification`.

## Architecture

```
modules/notification/
  application/
    NotificationDispatcher.java        # the module's own contract: one method per message
    mail/OrderConfirmationMail.java     # payload built from cross-module reads
    eventhandler/
      UserNotificationHandler.java      # user events  -> dispatcher
      OrderNotificationHandler.java     # OrderPaid     -> enrich -> dispatcher
      SubscriptionNotificationHandler.java
  infrastructure/
    event/NotificationEventListeners.java     # the single @TransactionalEventListener entry point
    mail/BrevoNotificationDispatcher.java     # NotificationDispatcher impl (Brevo + Thymeleaf)
    mail/BrevoProperties.java                 # @ConfigurationProperties("messages")
  interfaces/
    rest/ContactController.java               # POST /api/v1/contact (a contact form is a notification)
```

- **`NotificationDispatcher`** (application) is framework-free; it lists every
  message in business terms (`sendWelcomeEmail`, `sendOrderConfirmation`, …).
- **`BrevoNotificationDispatcher`** (infrastructure) is the only implementation:
  it renders the Thymeleaf template, resolves the subject via `MessageSource`,
  and POSTs to `https://api.brevo.com/v3/smtp/email`. Delivery is best-effort —
  a failure is logged, never propagated.
- **`NotificationEventListeners`** (infrastructure) holds all
  `@TransactionalEventListener(phase = AFTER_COMMIT)` methods and delegates to
  the application event handlers. It lives in `infrastructure` (not
  `interfaces`) because subscribing references other modules' `domain.event`
  types, which `interfacesMustNotDependOnModuleDomain` forbids in the
  interfaces layer.

## Triggers (events consumed)

All listeners fire on `AFTER_COMMIT`, so a message is only sent if the
originating transaction durably committed.

| Source event | Module | Handler | Email |
|--------------|--------|---------|-------|
| `UserRegistered` | user | UserNotificationHandler | Welcome |
| `UserPasswordChanged` | user | UserNotificationHandler | Password-changed security alert |
| `UserEmailChanged` | user | UserNotificationHandler | Alert to previous address |
| `PasswordResetRequested` | user | UserNotificationHandler | Reset link |
| `SuspiciousAuthActivityDetected` | user | UserNotificationHandler | Suspicious-activity alert |
| `LoginOtpRequested` | user | UserNotificationHandler | Login OTP code |
| `EmailChangeRequested` | user | UserNotificationHandler | Email-change confirmation link |
| `OrderPaid` | order | OrderNotificationHandler | Order confirmation |
| `SubscriptionCancelled` | subscription | SubscriptionNotificationHandler | Cancellation confirmation |
| `SubscriptionPaymentFailed` | subscription | SubscriptionNotificationHandler | Past-due / payment-failed alert |
| `SubscriptionAutoRenewReminderDue` | subscription | SubscriptionNotificationHandler | Auto-renew reminder |

### Two flavours of event

1. **Self-contained events** carry everything the message needs (e.g.
   `UserRegistered` has email/firstName/lang). The handler is a straight
   pass-through.
2. **Id-only events** (`OrderPaid`, `SubscriptionCancelled`,
   `SubscriptionPaymentFailed`) require the module to **enrich** by reading the
   producing module through its **published** `application.api`:
   - `OrderQueryApi.findOrderForConfirmation(orderId)` → totals + lines
   - `SubscriptionQueryApi.findForNotification(subscriptionId)` → product name
   - `UserQueryApi.findUserForNotification(userId)` → recipient email/name/lang

   These reads never touch another module's repository or schema — only its
   contract (the seam that becomes a remote call at extraction).

### Request-time sends modelled as events

Three sends are *caller-initiated* rather than reactions to a lifecycle change:
the **login OTP**, the **email-change confirmation**, and the **auto-renew
reminder**. To preserve the one-way dependency rule, the originating module
does **not** call notification — it publishes a domain event
(`LoginOtpRequested`, `EmailChangeRequested`, `SubscriptionAutoRenewReminderDue`)
inside its transaction, and notification reacts on `AFTER_COMMIT` like any
other event.

> Note: `LoginOtpRequested` carries the OTP code. This is an in-process Spring
> event (same trust boundary as the former direct method call) and never
> crosses the network. The publish happens inside the login transaction, so the
> code is only sent once the challenge is persisted.

## REST endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/contact` | public | Contact form → email to the configured team address + acknowledgement to the sender |

There are **no** customer/admin notification-history endpoints (see limitations).

## Configuration

Bound from the `messages.*` tree (see `application.yml`) into `BrevoProperties`:

| Property | Env var | Purpose |
|----------|---------|---------|
| `messages.api-key` | `BREVO_API_KEY` | Brevo API key (no send without it) |
| `messages.url` | `APP_PUBLIC_URL` | Base URL used to build links in emails |
| `messages.sender.{name,email}` | — | Verified Brevo sender |
| `messages.contact-to-email` | `CONTACT_TO_EMAIL` | Recipient of the contact form |

Templates live in `src/main/resources/templates/email/*.html`; subjects come
from `MessageSource` (`messages_fr`, `messages_en`).

## Known limitations

What this module intentionally does **not** do (and what the previous
aspirational spec wrongly described as present):

- **No persistence / no `notification_schema`.** Sends are fire-and-forget.
  There is no record that a message was sent and **no retry** on a Brevo
  failure — a failed send is logged and lost. Adding delivery guarantees would
  mean an outbox table (and therefore a schema), which is a deliberate
  follow-up, not part of this module today.
- **No notification history** for users or admins (no `GET /notifications`,
  no admin retry endpoint).
- **Email only.** No SMS/push channels.

## Related documents

- [Bounded Contexts](../domain/bounded-contexts.md)
- [Inter-Module Communication](../architecture/inter-module-communication.md)
- [Domain Events](../domain/domain-events.md)
- [Orders & Payments flow](../flows/order-payment-flow.md)
