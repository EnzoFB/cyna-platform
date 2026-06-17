package com.cyna.modules.order.domain.model;

import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Money;

import java.util.UUID;

public class OrderLine {

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 99;

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final String productCategory;
    private final BillingCycle billingCycle;
    private final int quantity;
    private final Money unitPrice;
    // Free-trial length snapshotted from the product at order time. Frozen on the
    // line (like unitPrice) so a later back-office change to the product's trial
    // never alters what an already-placed order granted. 0 = no trial.
    private final int freeTrialDays;

    private OrderLine(UUID id,
                      UUID productId,
                      String productName,
                      String productCategory,
                      BillingCycle billingCycle,
                      int quantity,
                      Money unitPrice,
                      int freeTrialDays) {
        Guard.againstNull(id, "id");
        Guard.againstNull(productId, "productId");
        Guard.againstNullOrBlank(productName, "productName");
        Guard.againstNullOrBlank(productCategory, "productCategory");
        Guard.againstNull(billingCycle, "billingCycle");
        Guard.againstNull(unitPrice, "unitPrice");
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be between 1 and 99");
        }
        if (freeTrialDays < 0) {
            throw new IllegalArgumentException("freeTrialDays must not be negative");
        }

        this.id = id;
        this.productId = productId;
        this.productName = productName.trim();
        this.productCategory = productCategory.trim();
        this.billingCycle = billingCycle;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.freeTrialDays = freeTrialDays;
    }

    public static OrderLine create(UUID productId,
                                   String productName,
                                   String productCategory,
                                   BillingCycle billingCycle,
                                   int quantity,
                                   Money unitPrice,
                                   int freeTrialDays) {
        return new OrderLine(UUID.randomUUID(), productId, productName, productCategory, billingCycle, quantity, unitPrice, freeTrialDays);
    }

    public static OrderLine reconstitute(UUID id,
                                         UUID productId,
                                         String productName,
                                         String productCategory,
                                         BillingCycle billingCycle,
                                         int quantity,
                                         Money unitPrice,
                                         int freeTrialDays) {
        return new OrderLine(id, productId, productName, productCategory, billingCycle, quantity, unitPrice, freeTrialDays);
    }

    public UUID getId() {
        return id;
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

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public int getFreeTrialDays() {
        return freeTrialDays;
    }
}
