package com.cyna.modules.payment.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class SavedPaymentMethod {

    private final UUID id;
    private final UUID userId;
    private final String stripePaymentMethodId;
    private final String brand;
    private final String last4;
    private final String expMonth;
    private final String expYear;
    private final String holderName;
    private final boolean isDefault;
    private final Instant createdAt;
    private final Instant updatedAt;

    private SavedPaymentMethod(UUID id, UUID userId, String stripePaymentMethodId,
                                String brand, String last4, String expMonth, String expYear,
                                String holderName, boolean isDefault,
                                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.stripePaymentMethodId = stripePaymentMethodId;
        this.brand = brand;
        this.last4 = last4;
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.holderName = holderName;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SavedPaymentMethod create(UUID userId, String stripePaymentMethodId,
                                             String brand, String last4,
                                             String expMonth, String expYear,
                                             String holderName, boolean isDefault) {
        Instant now = Instant.now();
        return new SavedPaymentMethod(UUID.randomUUID(), userId, stripePaymentMethodId,
                brand, last4, expMonth, expYear, holderName, isDefault, now, now);
    }

    public static SavedPaymentMethod reconstitute(UUID id, UUID userId, String stripePaymentMethodId,
                                                   String brand, String last4,
                                                   String expMonth, String expYear,
                                                   String holderName, boolean isDefault,
                                                   Instant createdAt, Instant updatedAt) {
        return new SavedPaymentMethod(id, userId, stripePaymentMethodId,
                brand, last4, expMonth, expYear, holderName, isDefault, createdAt, updatedAt);
    }

    public SavedPaymentMethod withDefault(boolean value) {
        return new SavedPaymentMethod(id, userId, stripePaymentMethodId,
                brand, last4, expMonth, expYear, holderName, value, createdAt, Instant.now());
    }

    /**
     * Returns a copy with refreshed display metadata — used when Stripe's
     * automatic card updater pushes a new last4/expiry for the same pm_xxx.
     * Identity (id, userId, isDefault, createdAt) is preserved; updatedAt is
     * stamped to now.
     */
    public SavedPaymentMethod withRefreshedCardDetails(
            String newBrand, String newLast4, String newExpMonth, String newExpYear) {
        return new SavedPaymentMethod(id, userId, stripePaymentMethodId,
                newBrand, newLast4, newExpMonth, newExpYear,
                holderName, isDefault, createdAt, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getStripePaymentMethodId() { return stripePaymentMethodId; }
    public String getBrand() { return brand; }
    public String getLast4() { return last4; }
    public String getExpMonth() { return expMonth; }
    public String getExpYear() { return expYear; }
    public String getHolderName() { return holderName; }
    public boolean isDefault() { return isDefault; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
