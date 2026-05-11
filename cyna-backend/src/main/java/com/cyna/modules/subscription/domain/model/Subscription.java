package com.cyna.modules.subscription.domain.model;

import com.cyna.modules.subscription.domain.event.SubscriptionActivated;
import com.cyna.modules.subscription.domain.event.SubscriptionCancelled;
import com.cyna.modules.subscription.domain.event.SubscriptionRenewed;
import com.cyna.shared.domain.AggregateRoot;
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
    private final String stripeSubscriptionId;
    private final String stripeScheduleId;
    private final boolean autoRenew;
    private final Instant autoRenewNoticeSentAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Subscription(UUID id,
                         UUID userId,
                         UUID orderId,
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
                         String stripeSubscriptionId,
                         String stripeScheduleId,
                         boolean autoRenew,
                         Instant autoRenewNoticeSentAt,
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
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeScheduleId = stripeScheduleId;
        this.autoRenew = autoRenew;
        this.autoRenewNoticeSentAt = autoRenewNoticeSentAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Subscription createActive(UUID userId,
                                            UUID orderId,
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
                stripeSubscriptionId,
                stripeScheduleId,
                true,
                null,
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
                                            String stripeSubscriptionId,
                                            String stripeScheduleId,
                                            boolean autoRenew,
                                            Instant autoRenewNoticeSentAt,
                                            Instant createdAt,
                                            Instant updatedAt) {
        return new Subscription(
                id,
                userId,
                orderId,
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
                stripeSubscriptionId,
                stripeScheduleId,
                autoRenew,
                autoRenewNoticeSentAt,
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
        Subscription renewed = new Subscription(
                getId(),
                userId,
                orderId,
                productId,
                productName,
                productCategory,
                billingCycle,
                SubscriptionStatus.ACTIVE,
                quantity,
                unitPrice,
                startAt,
                newEndAt,
                newNextBillingAt,
                cancelledAt,
                stripeSubscriptionId,
                stripeScheduleId,
                autoRenew,
                null,
                createdAt,
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
        return Result.success(new Subscription(
                getId(),
                userId,
                orderId,
                productId,
                productName,
                productCategory,
                billingCycle,
                SubscriptionStatus.PAST_DUE,
                quantity,
                unitPrice,
                startAt,
                endAt,
                nextBillingAt,
                cancelledAt,
                stripeSubscriptionId,
                stripeScheduleId,
                autoRenew,
                autoRenewNoticeSentAt,
                createdAt,
                now
        ));
    }

    public Result<Subscription> cancelAtPeriodEnd() {
        if (status == SubscriptionStatus.CANCELLED) {
            return Result.failure("Subscription already cancelled");
        }
        if (status == SubscriptionStatus.EXPIRED) {
            return Result.failure("Subscription already expired");
        }

        Instant now = Instant.now();
        Subscription cancelled = new Subscription(
                getId(),
                userId,
                orderId,
                productId,
                productName,
                productCategory,
                billingCycle,
                SubscriptionStatus.CANCELLED,
                quantity,
                unitPrice,
                startAt,
                endAt,
                nextBillingAt,
                now,
                stripeSubscriptionId,
                stripeScheduleId,
                autoRenew,
                autoRenewNoticeSentAt,
                createdAt,
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
        return Result.success(new Subscription(
                getId(), userId, orderId, productId, productName, productCategory,
                billingCycle, status, quantity, unitPrice,
                startAt, endAt, nextBillingAt, cancelledAt,
                stripeSubscriptionId, stripeScheduleId,
                newAutoRenew, newAutoRenew ? autoRenewNoticeSentAt : null,
                createdAt, now
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

        Instant now = Instant.now();
        return Result.success(new Subscription(
                getId(), userId, orderId, productId, productName, productCategory,
                billingCycle, status, quantity, unitPrice,
                startAt, endAt, nextBillingAt, cancelledAt,
                stripeSubscriptionId, stripeScheduleId,
                autoRenew, sentAt,
                createdAt, now
        ));
    }

    public UUID getUserId() { return userId; }
    public UUID getOrderId() { return orderId; }
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
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public String getStripeScheduleId() { return stripeScheduleId; }
    public boolean isAutoRenew() { return autoRenew; }
    public Instant getAutoRenewNoticeSentAt() { return autoRenewNoticeSentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
