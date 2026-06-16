package com.cyna.modules.subscription.domain.model;

import com.cyna.modules.subscription.domain.event.SubscriptionActivated;
import com.cyna.modules.subscription.domain.event.SubscriptionCancelled;
import com.cyna.modules.subscription.domain.event.SubscriptionPaymentActionRequired;
import com.cyna.modules.subscription.domain.event.SubscriptionPaymentFailed;
import com.cyna.modules.subscription.domain.event.SubscriptionRenewed;
import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;

import java.time.Instant;
import java.util.UUID;

public class Subscription extends AggregateRoot<UUID> {

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 99;

    private final UUID userId;
    private final UUID orderId;
    // Identifies which OrderLine this Subscription was provisioned for. Required for the
    // multi-product mixed-cycle checkout (V14+): each line gets its own Stripe Subscription,
    // and we use the line id to keep the local mirror 1-to-1 with Stripe. NULL for pre-V14
    // subscriptions seeded under the legacy one-Stripe-sub-per-order model.
    private final UUID orderLineId;
    private final UUID productId;
    private final String productName;
    private final String productCategory;
    private final BillingCycle billingCycle;
    private final SubscriptionStatus status;
    private final int quantity;
    private final Money unitPrice;
    private final Instant startAt;
    private final Instant endAt;
    private final Instant nextBillingAt;
    private final Instant cancelledAt;
    private final boolean autoRenew;
    private final Instant autoRenewNoticeSentAt;
    private final String stripeSubscriptionId;
    private final String stripeScheduleId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Subscription(UUID id,
                         UUID userId,
                         UUID orderId,
                         UUID orderLineId,
                         UUID productId,
                         String productName,
                         String productCategory,
                         BillingCycle billingCycle,
                         SubscriptionStatus status,
                         int quantity,
                         Money unitPrice,
                         Instant startAt,
                         Instant endAt,
                         Instant nextBillingAt,
                         Instant cancelledAt,
                         boolean autoRenew,
                         Instant autoRenewNoticeSentAt,
                         String stripeSubscriptionId,
                         String stripeScheduleId,
                         Instant createdAt,
                         Instant updatedAt) {
        super(id);
        Guard.againstNull(id, "id");
        Guard.againstNull(userId, "userId");
        Guard.againstNull(orderId, "orderId");
        Guard.againstNull(productId, "productId");
        Guard.againstNullOrBlank(productName, "productName");
        Guard.againstNullOrBlank(productCategory, "productCategory");
        Guard.againstNull(billingCycle, "billingCycle");
        Guard.againstNull(status, "status");
        Guard.againstNull(unitPrice, "unitPrice");
        Guard.againstNull(startAt, "startAt");
        Guard.againstNull(endAt, "endAt");
        Guard.againstNull(nextBillingAt, "nextBillingAt");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be between 1 and 99");
        }
        if (endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        this.userId = userId;
        this.orderId = orderId;
        this.orderLineId = orderLineId;
        this.productId = productId;
        this.productName = productName.trim();
        this.productCategory = productCategory.trim();
        this.billingCycle = billingCycle;
        this.status = status;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.startAt = startAt;
        this.endAt = endAt;
        this.nextBillingAt = nextBillingAt;
        this.cancelledAt = cancelledAt;
        this.autoRenew = autoRenew;
        this.autoRenewNoticeSentAt = autoRenewNoticeSentAt;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeScheduleId = stripeScheduleId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Subscription createActive(UUID userId,
                                            UUID orderId,
                                            UUID orderLineId,
                                            UUID productId,
                                            String productName,
                                            String productCategory,
                                            BillingCycle billingCycle,
                                            int quantity,
                                            Money unitPrice,
                                            Instant startAt,
                                            Instant endAt,
                                            Instant nextBillingAt,
                                            String stripeSubscriptionId,
                                            String stripeScheduleId) {
        Instant now = Instant.now();
        Subscription subscription = new Subscription(
                UUID.randomUUID(),
                userId,
                orderId,
                orderLineId,
                productId,
                productName,
                productCategory,
                billingCycle,
                SubscriptionStatus.ACTIVE,
                quantity,
                unitPrice,
                startAt,
                endAt,
                nextBillingAt,
                null,
                true,
                null,
                stripeSubscriptionId,
                stripeScheduleId,
                now,
                now
        );
        subscription.raise(new SubscriptionActivated(
                subscription.getId(),
                userId,
                orderId,
                productId,
                now
        ));
        return subscription;
    }

    public static Subscription reconstitute(UUID id,
                                            UUID userId,
                                            UUID orderId,
                                            UUID orderLineId,
                                            UUID productId,
                                            String productName,
                                            String productCategory,
                                            BillingCycle billingCycle,
                                            SubscriptionStatus status,
                                            int quantity,
                                            Money unitPrice,
                                            Instant startAt,
                                            Instant endAt,
                                            Instant nextBillingAt,
                                            Instant cancelledAt,
                                            boolean autoRenew,
                                            Instant autoRenewNoticeSentAt,
                                            String stripeSubscriptionId,
                                            String stripeScheduleId,
                                            Instant createdAt,
                                            Instant updatedAt) {
        return new Subscription(
                id,
                userId,
                orderId,
                orderLineId,
                productId,
                productName,
                productCategory,
                billingCycle,
                status,
                quantity,
                unitPrice,
                startAt,
                endAt,
                nextBillingAt,
                cancelledAt,
                autoRenew,
                autoRenewNoticeSentAt,
                stripeSubscriptionId,
                stripeScheduleId,
                createdAt,
                updatedAt
        );
    }

    public Result<Subscription> renew(Instant newEndAt, Instant newNextBillingAt) {
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.PAST_DUE) {
            return Result.failure("Cannot renew subscription with status " + status);
        }
        if (newEndAt == null || newNextBillingAt == null) {
            return Result.failure("Renewal dates are required");
        }
        if (newEndAt.isBefore(endAt)) {
            return Result.failure("Renewal end date must not move backwards");
        }

        Instant now = Instant.now();
        Subscription renewed = copyWith(
                SubscriptionStatus.ACTIVE,
                newEndAt,
                newNextBillingAt,
                cancelledAt,
                autoRenew,
                autoRenewNoticeSentAt,
                now
        );
        renewed.raise(new SubscriptionRenewed(
                getId(),
                userId,
                orderId,
                productId,
                now
        ));
        return Result.success(renewed);
    }

    public Result<Subscription> markPastDue() {
        if (status == SubscriptionStatus.CANCELLED || status == SubscriptionStatus.EXPIRED) {
            return Result.failure("Cannot mark a terminated subscription as past due");
        }
        if (status == SubscriptionStatus.PAST_DUE) {
            return Result.success(this);
        }
        Instant now = Instant.now();
        Subscription pastDue = copyWith(
                SubscriptionStatus.PAST_DUE,
                endAt,
                nextBillingAt,
                cancelledAt,
                autoRenew,
                autoRenewNoticeSentAt,
                now
        );
        pastDue.raise(new SubscriptionPaymentFailed(
                getId(),
                userId,
                orderId,
                productId,
                now
        ));
        return Result.success(pastDue);
    }

    /**
     * Renewal failed because the customer's bank requires SCA (PSD2). The
     * subscription transitions to PAST_DUE just like {@link #markPastDue()},
     * and the raised event carries the Stripe-hosted invoice URL so the
     * notification module can email a "complete 3DS here" link — that's the
     * only thing the customer needs to do to recover (no card replacement, no
     * support ticket).
     *
     * <p>The {@code PAST_DUE → PAST_DUE} no-op guard is symmetric with
     * {@link #markPastDue()}: Stripe re-emits {@code invoice.payment_action_required}
     * at every dunning retry (24-72h cadence). Without the short-circuit the
     * customer would receive a new "complete 3DS" email at every retry — exactly
     * the dunning spam the email is meant to break. The first email IS sent on
     * the {@code ACTIVE → PAST_DUE} transition; subsequent retries while still
     * PAST_DUE are silenced. When the customer eventually completes 3DS, the
     * subscription returns to ACTIVE via the {@code customer.subscription.updated}
     * sync path, so a future cycle that fails again will legitimately re-fire
     * the event (it transitions ACTIVE → PAST_DUE anew).
     */
    public Result<Subscription> markPaymentActionRequired(String hostedInvoiceUrl) {
        if (status == SubscriptionStatus.CANCELLED || status == SubscriptionStatus.EXPIRED) {
            return Result.failure("Cannot mark a terminated subscription as payment-action-required");
        }
        if (status == SubscriptionStatus.PAST_DUE) {
            return Result.success(this);
        }
        Instant now = Instant.now();
        Subscription pastDue = copyWith(
                SubscriptionStatus.PAST_DUE,
                endAt,
                nextBillingAt,
                cancelledAt,
                autoRenew,
                autoRenewNoticeSentAt,
                now
        );
        pastDue.raise(new SubscriptionPaymentActionRequired(
                getId(),
                userId,
                orderId,
                productId,
                hostedInvoiceUrl,
                now
        ));
        return Result.success(pastDue);
    }

    // User-initiated "cancel at period end" — equivalent to disabling auto-renew on Stripe
    // (cancel_at_period_end=true). The subscription stays ACTIVE until Stripe definitively
    // cancels at the end of the current period, at which point the webhook handler will
    // call markFullyCancelled(). This preserves access for the period the customer paid for.
    public Result<Subscription> cancelAtPeriodEnd() {
        return updateAutoRenew(false);
    }

    // Called by the webhook handler on `customer.subscription.deleted` — Stripe has
    // definitively cancelled the subscription (either at period end after a user cancel,
    // or immediately on hard failure / admin action). This is the terminal state.
    public Result<Subscription> markFullyCancelled() {
        if (status == SubscriptionStatus.CANCELLED) {
            return Result.success(this);
        }
        if (status == SubscriptionStatus.EXPIRED) {
            return Result.failure("Subscription already expired");
        }

        Instant now = Instant.now();
        Instant resolvedCancelledAt = cancelledAt != null ? cancelledAt : now;
        Subscription cancelled = copyWith(
                SubscriptionStatus.CANCELLED,
                endAt,
                nextBillingAt,
                resolvedCancelledAt,
                false,
                autoRenewNoticeSentAt,
                now
        );
        cancelled.raise(new SubscriptionCancelled(
                getId(),
                userId,
                orderId,
                productId,
                now
        ));
        return Result.success(cancelled);
    }

    public Result<Subscription> updateAutoRenew(boolean newAutoRenew) {
        if (status == SubscriptionStatus.CANCELLED || status == SubscriptionStatus.EXPIRED) {
            return Result.failure("Cannot update auto-renew for terminated subscription");
        }
        if (this.autoRenew == newAutoRenew) {
            return Result.success(this);
        }

        Instant now = Instant.now();
        // When auto-renew is re-enabled, drop any prior "cancellation requested at" marker
        // and any sent-reminder marker (a fresh reminder will be sent for the next cycle).
        Instant newCancelledAt = newAutoRenew ? null : now;
        Instant newNoticeSentAt = newAutoRenew ? null : autoRenewNoticeSentAt;

        return Result.success(copyWith(
                status,
                endAt,
                nextBillingAt,
                newCancelledAt,
                newAutoRenew,
                newNoticeSentAt,
                now
        ));
    }

    /**
     * Reconciles the local subscription with the authoritative state held by Stripe,
     * carried by {@code customer.subscription.*} webhooks. The webhook handler calls
     * this for every relevant event so the local mirror never drifts — whether the
     * change came from us, from the customer portal, from the Stripe dashboard, or
     * from Stripe's own lifecycle (period end, dunning, automatic cancel…).
     *
     * <p>Stripe statuses are mapped as follows:
     * <ul>
     *   <li>{@code active}, {@code trialing} → {@link SubscriptionStatus#ACTIVE}</li>
     *   <li>{@code past_due}, {@code unpaid} → {@link SubscriptionStatus#PAST_DUE}</li>
     *   <li>{@code canceled} → {@link SubscriptionStatus#CANCELLED} (terminal,
     *       raises {@link SubscriptionCancelled})</li>
     *   <li>Other transitional states ({@code incomplete},
     *       {@code incomplete_expired}) are ignored — those are pre-activation states
     *       handled by the initial payment flow.</li>
     * </ul>
     *
     * <p>{@code cancel_at_period_end} is mirrored to {@code !autoRenew}. A null value
     * is treated as "unknown — leave as is".
     */
    public Result<Subscription> syncFromStripeState(String stripeStatus,
                                                    Boolean cancelAtPeriodEnd,
                                                    Instant currentPeriodEnd,
                                                    Instant stripeCanceledAt) {
        if ("canceled".equals(stripeStatus)) {
            return markFullyCancelled();
        }

        Subscription draft = this;

        if ("past_due".equals(stripeStatus) || "unpaid".equals(stripeStatus)) {
            Result<Subscription> r = draft.markPastDue();
            if (r.isFailure()) {
                return r;
            }
            draft = r.getValue();
        } else if ("active".equals(stripeStatus) || "trialing".equals(stripeStatus)) {
            // Recover from PAST_DUE if Stripe says we're active again.
            if (draft.status == SubscriptionStatus.PAST_DUE) {
                Result<Subscription> r = draft.copyWithStatus(SubscriptionStatus.ACTIVE);
                if (r.isFailure()) {
                    return r;
                }
                draft = r.getValue();
            }
        }
        // Other Stripe statuses (incomplete, incomplete_expired, paused) are no-ops here.

        if (cancelAtPeriodEnd != null) {
            boolean desiredAutoRenew = !cancelAtPeriodEnd;
            if (draft.autoRenew != desiredAutoRenew) {
                Result<Subscription> r = draft.updateAutoRenew(desiredAutoRenew);
                if (r.isSuccess()) {
                    draft = r.getValue();
                }
                // updateAutoRenew failures (terminated sub) are silently ignored here —
                // the markFullyCancelled branch above already handled that case.
            }
        }

        if (currentPeriodEnd != null && currentPeriodEnd.isAfter(draft.endAt)) {
            Result<Subscription> r = draft.renew(currentPeriodEnd, currentPeriodEnd);
            if (r.isSuccess()) {
                draft = r.getValue();
            }
        }

        return Result.success(draft);
    }

    private Result<Subscription> copyWithStatus(SubscriptionStatus newStatus) {
        if (this.status == newStatus) {
            return Result.success(this);
        }
        return Result.success(copyWith(
                newStatus,
                endAt,
                nextBillingAt,
                cancelledAt,
                autoRenew,
                autoRenewNoticeSentAt,
                Instant.now()
        ));
    }

    public Result<Subscription> markAutoRenewNoticeSent(Instant sentAt) {
        if (!autoRenew) {
            return Result.failure("Auto-renew is disabled");
        }
        if (status != SubscriptionStatus.ACTIVE) {
            return Result.failure("Only active subscriptions can receive auto-renew notices");
        }
        if (sentAt == null) {
            return Result.failure("Notice timestamp is required");
        }
        if (autoRenewNoticeSentAt != null && !sentAt.isAfter(autoRenewNoticeSentAt)) {
            return Result.failure("Notice timestamp must be newer than the previous one");
        }

        return Result.success(copyWith(
                status,
                endAt,
                nextBillingAt,
                cancelledAt,
                autoRenew,
                sentAt,
                sentAt
        ));
    }

    private Subscription copyWith(SubscriptionStatus newStatus,
                                  Instant newEndAt,
                                  Instant newNextBillingAt,
                                  Instant newCancelledAt,
                                  boolean newAutoRenew,
                                  Instant newAutoRenewNoticeSentAt,
                                  Instant newUpdatedAt) {
        return new Subscription(
                getId(),
                userId,
                orderId,
                orderLineId,
                productId,
                productName,
                productCategory,
                billingCycle,
                newStatus,
                quantity,
                unitPrice,
                startAt,
                newEndAt,
                newNextBillingAt,
                newCancelledAt,
                newAutoRenew,
                newAutoRenewNoticeSentAt,
                stripeSubscriptionId,
                stripeScheduleId,
                createdAt,
                newUpdatedAt
        );
    }

    public UUID getUserId() { return userId; }
    public UUID getOrderId() { return orderId; }
    public UUID getOrderLineId() { return orderLineId; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductCategory() { return productCategory; }
    public BillingCycle getBillingCycle() { return billingCycle; }
    public SubscriptionStatus getStatus() { return status; }
    public int getQuantity() { return quantity; }
    public Money getUnitPrice() { return unitPrice; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public Instant getNextBillingAt() { return nextBillingAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public boolean isAutoRenew() { return autoRenew; }
    public Instant getAutoRenewNoticeSentAt() { return autoRenewNoticeSentAt; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public String getStripeScheduleId() { return stripeScheduleId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
