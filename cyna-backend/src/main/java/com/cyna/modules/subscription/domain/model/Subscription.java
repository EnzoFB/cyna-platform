package com.cyna.modules.subscription.domain.model;

import com.cyna.modules.subscription.domain.event.SubscriptionActivated;
import com.cyna.modules.subscription.domain.event.SubscriptionCancelled;
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
                                            Instant nextBillingAt) {
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
                createdAt,
                updatedAt
        );
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

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public Instant getNextBillingAt() {
        return nextBillingAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
