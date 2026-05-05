package com.cyna.modules.payment.domain.model;

import com.cyna.modules.payment.domain.event.PaymentFailed;
import com.cyna.modules.payment.domain.event.PaymentInitiated;
import com.cyna.modules.payment.domain.event.PaymentSucceeded;
import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;

import java.time.Instant;
import java.util.UUID;

public class Payment extends AggregateRoot<UUID> {

    private final UUID orderId;
    private final UUID userId;
    private final PaymentStatus status;
    private final Money amount;
    private final String stripePaymentIntentId;
    private final String stripeClientSecret;
    private final String stripeSubscriptionId;
    private final String stripeScheduleId;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Payment(UUID id,
                    UUID orderId,
                    UUID userId,
                    PaymentStatus status,
                    Money amount,
                    String stripePaymentIntentId,
                    String stripeClientSecret,
                    String stripeSubscriptionId,
                    String stripeScheduleId,
                    Instant createdAt,
                    Instant updatedAt) {
        super(id);
        Guard.againstNull(id, "id");
        Guard.againstNull(orderId, "orderId");
        Guard.againstNull(userId, "userId");
        Guard.againstNull(status, "status");
        Guard.againstNull(amount, "amount");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.amount = amount;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.stripeClientSecret = stripeClientSecret;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeScheduleId = stripeScheduleId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment create(UUID id, UUID orderId, UUID userId, Money amount) {
        Instant now = Instant.now();
        Payment payment = new Payment(id, orderId, userId, PaymentStatus.PENDING,
                amount, null, null, null, null, now, now);
        payment.raise(new PaymentInitiated(id, orderId, userId, amount.amount(), now));
        return payment;
    }

    public static Payment reconstitute(UUID id, UUID orderId, UUID userId, PaymentStatus status,
                                       Money amount, String stripePaymentIntentId,
                                       String stripeClientSecret, String stripeSubscriptionId,
                                       String stripeScheduleId, Instant createdAt, Instant updatedAt) {
        return new Payment(id, orderId, userId, status, amount,
                stripePaymentIntentId, stripeClientSecret, stripeSubscriptionId, stripeScheduleId,
                createdAt, updatedAt);
    }

    public Payment assignStripeSubscription(String paymentIntentId, String clientSecret,
                                            String subscriptionId, String scheduleId) {
        Guard.againstNullOrBlank(paymentIntentId, "paymentIntentId");
        Guard.againstNullOrBlank(clientSecret, "clientSecret");
        Guard.againstNullOrBlank(subscriptionId, "subscriptionId");
        return new Payment(getId(), orderId, userId, PaymentStatus.PENDING, amount,
                paymentIntentId, clientSecret, subscriptionId, scheduleId, createdAt, Instant.now());
    }

    public Result<Payment> markSucceeded() {
        if (status == PaymentStatus.SUCCEEDED) {
            return Result.failure("Payment is already succeeded");
        }
        if (status == PaymentStatus.REFUNDED) {
            return Result.failure("Cannot change a refunded payment");
        }

        Instant now = Instant.now();
        Payment succeeded = new Payment(getId(), orderId, userId, PaymentStatus.SUCCEEDED,
                amount, stripePaymentIntentId, stripeClientSecret,
                stripeSubscriptionId, stripeScheduleId, createdAt, now);
        succeeded.raise(new PaymentSucceeded(getId(), orderId, userId, amount.amount(), stripeSubscriptionId, now));
        return Result.success(succeeded);
    }

    public Result<Payment> markFailed() {
        if (status == PaymentStatus.SUCCEEDED) {
            return Result.failure("Cannot fail a succeeded payment");
        }
        if (status == PaymentStatus.FAILED) {
            return Result.failure("Payment is already failed");
        }

        Instant now = Instant.now();
        Payment failed = new Payment(getId(), orderId, userId, PaymentStatus.FAILED,
                amount, stripePaymentIntentId, stripeClientSecret,
                stripeSubscriptionId, stripeScheduleId, createdAt, now);
        failed.raise(new PaymentFailed(getId(), orderId, userId, now));
        return Result.success(failed);
    }

    public UUID getOrderId() { return orderId; }
    public UUID getUserId() { return userId; }
    public PaymentStatus getStatus() { return status; }
    public Money getAmount() { return amount; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public String getStripeClientSecret() { return stripeClientSecret; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public String getStripeScheduleId() { return stripeScheduleId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
