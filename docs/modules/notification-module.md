# Notification Module

## Purpose

This document describes the **Notification** module of the CYNA Platform. This module is responsible for sending notifications to users across multiple channels (email, SMS, push) in response to domain events from all other modules.

---

## Overview

| Property | Value |
|----------|-------|
| Module name | `notification` |
| Bounded context | Notification |
| Base package | `com.cyna.modules.notification` |
| Database schema | `notification_schema` |
| Primary aggregate | `Notification` (lightweight) |

The Notification module is an **event-driven** module. It does not initiate any business process — it exclusively reacts to domain events published by other modules. It uses a thin domain model because its core behavior is coordination and delivery rather than complex business rule enforcement.

---

## Domain Model

### Aggregate: Notification

| Field | Type | Description |
|-------|------|-------------|
| `id` | `NotificationId` | Unique notification identifier |
| `recipientId` | `RecipientId` | Reference to the recipient user |
| `recipientEmail` | `String` | Recipient email address |
| `type` | `NotificationType` | Category of the notification |
| `channel` | `Channel` | Delivery channel |
| `subject` | `String` | Notification subject |
| `body` | `String` | Notification body (HTML for email) |
| `status` | `NotificationStatus` | Current delivery state (default: `PENDING`) |
| `failureReason` | `String` | Failure detail (when failed) |
| `retryCount` | `int` | Number of retry attempts (max 3) |
| `createdAt` | `Instant` | Creation timestamp |
| `sentAt` | `Instant` | Sent timestamp (when delivered) |

**Behaviors:**

| Method | Description |
|--------|-------------|
| `create(recipientId, recipientEmail, type, channel, subject, body)` | Factory — creates a pending notification |
| `markSent()` | Marks as sent, sets `sentAt` timestamp |
| `markFailed(reason)` | Marks as failed, increments `retryCount` |
| `canRetry()` | Returns `true` if `retryCount < 3` and status is `FAILED` |
| `markRetrying()` | Resets status to `PENDING` for retry |

### Value Objects & Enums

| Value Object | Description | Values |
|-------------|-------------|--------|
| `NotificationId` | Unique identifier | Non-null UUID |
| `RecipientId` | Reference to the recipient | Non-null UUID |
| `NotificationType` | Category | `WELCOME_EMAIL`, `PASSWORD_CHANGED`, `ACCOUNT_ACTIVATED`, `ACCOUNT_DEACTIVATED`, `ORDER_CONFIRMED`, `ORDER_FULFILLED`, `ORDER_CANCELLED`, `PAYMENT_SUCCEEDED`, `PAYMENT_FAILED`, `REFUND_COMPLETED`, `INVOICE_GENERATED`, `SUBSCRIPTION_ACTIVATED`, `SUBSCRIPTION_RENEWED`, `SUBSCRIPTION_CANCELLED`, `SUBSCRIPTION_EXPIRED`, `SUBSCRIPTION_SUSPENDED`, `RENEWAL_REMINDER` |
| `Channel` | Delivery channel | `EMAIL`, `SMS`, `PUSH` |
| `NotificationStatus` | Delivery state | `PENDING`, `SENT`, `FAILED` |

### Port Interfaces

| Port | Methods | Description |
|------|---------|-------------|
| `EmailSenderPort` | `send(to, subject, body)` → `SendResult` | Sends an email |
| `SmsSenderPort` | `send(phoneNumber, message)` → `SendResult` | Sends an SMS |
| `PushNotificationPort` | `send(deviceToken, title, message)` → `SendResult` | Sends a push notification |
| `NotificationTemplateService` | `resolve(type, templateData)` → `TemplateResult(subject, body)` | Resolves notification content from templates |

### Repository Interface

| Method | Description |
|--------|-------------|
| `save(Notification)` | Persist a notification |
| `findById(NotificationId)` | Find notification by ID |
| `findByRecipientId(RecipientId)` | Find notifications for a user |
| `findByStatus(NotificationStatus)` | Find notifications by status |
| `findFailedRetriable()` | Find failed notifications eligible for retry |

---

## Application Layer

### Commands

#### `SendNotificationCommand` → `SendNotificationCommandHandler`

Creates and sends a notification. Resolves template content, attempts delivery, and records the result.

| Field | Type | Validation |
|-------|------|------------|
| `recipientId` | `UUID` | Required |
| `recipientEmail` | `String` | Optional (may be resolved from user data) |
| `type` | `String` | Required, must match `NotificationType` enum |
| `channel` | `String` | Required, must match `Channel` enum |
| `templateData` | `Map<String, Object>` | Required, variables for template resolution |

**Returns:** `Result<NotificationId>`

**Flow:** Resolve template → create `Notification` → attempt delivery via appropriate port → mark sent or failed → save.

#### `RetryFailedNotificationsCommand` → `RetryFailedNotificationsCommandHandler`

Retries all failed notifications that are eligible for retry (retryCount < 3).

No input fields.

**Returns:** `Result<Void>`

### Queries

#### `GetNotificationByIdQuery` → `GetNotificationByIdQueryHandler`

Retrieves a notification by ID.

| Field | Type |
|-------|------|
| `notificationId` | `UUID` |

**Returns:** `NotificationReadModel` — id, recipientId, type, channel, subject, status, failureReason, retryCount, createdAt, sentAt

#### `ListNotificationsByRecipientQuery` → `ListNotificationsByRecipientQueryHandler`

Lists notifications for a user.

| Field | Type |
|-------|------|
| `recipientId` | `UUID` |

**Returns:** `List<NotificationReadModel>`

### Event Handlers

All event handlers use `@TransactionalEventListener(phase = AFTER_COMMIT)` to ensure notifications are only sent when the originating transaction has been committed successfully.

| Handler | Listens to | Notification Type | Channel |
|---------|-----------|-------------------|---------|
| `OnUserRegisteredHandler` | `UserRegistered` | `WELCOME_EMAIL` | EMAIL |
| `OnPasswordChangedHandler` | `PasswordChanged` | `PASSWORD_CHANGED` | EMAIL |
| `OnUserActivatedHandler` | `UserActivated` | `ACCOUNT_ACTIVATED` | EMAIL |
| `OnUserDeactivatedHandler` | `UserDeactivated` | `ACCOUNT_DEACTIVATED` | EMAIL |
| `OnOrderConfirmedHandler` | `OrderConfirmed` | `ORDER_CONFIRMED` | EMAIL |
| `OnOrderFulfilledHandler` | `OrderFulfilled` | `ORDER_FULFILLED` | EMAIL |
| `OnOrderCancelledHandler` | `OrderCancelled` | `ORDER_CANCELLED` | EMAIL |
| `PaymentSucceededReconciliationHandler` | `PaymentSucceeded` | `PAYMENT_SUCCEEDED` | EMAIL |
| `OnPaymentFailedHandler` | `PaymentFailed` | `PAYMENT_FAILED` | EMAIL |
| `OnRefundCompletedHandler` | `RefundCompleted` | `REFUND_COMPLETED` | EMAIL |
| `OnInvoiceGeneratedHandler` | `InvoiceGenerated` | `INVOICE_GENERATED` | EMAIL |
| `OnSubscriptionActivatedHandler` | `SubscriptionActivated` | `SUBSCRIPTION_ACTIVATED` | EMAIL |
| `OnSubscriptionRenewedHandler` | `SubscriptionRenewed` | `SUBSCRIPTION_RENEWED` | EMAIL |
| `OnSubscriptionCancelledHandler` | `SubscriptionCancelled` | `SUBSCRIPTION_CANCELLED` | EMAIL |
| `OnSubscriptionExpiredHandler` | `SubscriptionExpired` | `SUBSCRIPTION_EXPIRED` | EMAIL |
| `OnSubscriptionSuspendedHandler` | `SubscriptionSuspended` | `SUBSCRIPTION_SUSPENDED` | EMAIL |

Each handler dispatches a `SendNotificationCommand` with the appropriate type, channel, and template data extracted from the event payload.

---

## REST Endpoints

### Customer-Facing

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/notifications` | CUSTOMER | List own notifications |
| `GET` | `/api/v1/notifications/{id}` | CUSTOMER | Get notification details |

### Backoffice

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/admin/notifications` | ADMIN, SUPPORT | List all notifications |
| `GET` | `/api/v1/admin/notifications/{id}` | ADMIN, SUPPORT | Get any notification details |
| `POST` | `/api/v1/admin/notifications/retry` | ADMIN | Retry all failed notifications |

### Response Body — `NotificationResponse`

| Field | Type |
|-------|------|
| `id` | `UUID` |
| `recipientId` | `UUID` |
| `type` | `String` |
| `channel` | `String` |
| `subject` | `String` |
| `status` | `String` |
| `failureReason` | `String` |
| `retryCount` | `int` |
| `createdAt` | `Instant` |
| `sentAt` | `Instant` |

---

## Scheduled Jobs

| Job | Schedule | Command triggered | Description |
|-----|----------|-------------------|-------------|
| Retry failed notifications | Every 15 minutes | `RetryFailedNotificationsCommand` | Retries failed notifications up to 3 times |
| Renewal reminders | Daily | `SendNotificationCommand` | Sends reminders for subscriptions approaching renewal |

---

## Infrastructure Adapters

| Adapter | Implements | Technology | Description |
|---------|-----------|------------|-------------|
| `SmtpEmailAdapter` | `EmailSenderPort` | Spring `JavaMailSender` | Sends HTML emails via SMTP. Configured via `notification.email.from` property. |
| SMS adapter | `SmsSenderPort` | TBD | To be implemented |
| Push adapter | `PushNotificationPort` | TBD | To be implemented |

---

## Module Dependencies

| Direction | Module | Mechanism | Description |
|-----------|--------|-----------|-------------|
| **Reacts to** | User (IAM) | Domain events (async) | Sends welcome email, password change, account status notifications |
| **Reacts to** | Order | Domain events (async) | Sends order confirmation, fulfillment, cancellation notifications |
| **Reacts to** | Payment | Domain events (async) | Sends payment receipts, failure alerts, refund/invoice notifications |
| **Reacts to** | Subscription | Domain events (async) | Sends subscription lifecycle notifications |

---

## Business Rules

| Rule | Enforcement |
|------|-------------|
| Notifications are only sent after the source transaction commits | `@TransactionalEventListener(AFTER_COMMIT)` |
| Maximum 3 retry attempts for failed notifications | `canRetry()` check in aggregate |
| Notification content is resolved from templates | `NotificationTemplateService` |
| Email is the default channel for all notification types | Event handler configuration |
| Notification module never initiates business processes | Event-driven architecture only |

---

## Related Documents

- [Bounded Contexts](../domain/bounded-contexts.md)
- [Module Structure](../architecture/module-structure.md)
- [Inter-Module Communication](../architecture/inter-module-communication.md)
- [Domain Events](../domain/domain-events.md)
