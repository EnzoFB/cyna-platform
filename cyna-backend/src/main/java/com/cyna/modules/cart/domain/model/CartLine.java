package com.cyna.modules.cart.domain.model;

import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.domain.Entity;
import com.cyna.shared.domain.Guard;

import java.util.UUID;

public class CartLine extends Entity<UUID> {

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 99;

    private final UUID productId;
    private final String productName;
    private final String productCategory;
    private final BillingCycle billingCycle;
    private final int quantity;

    private CartLine(UUID id,
                     UUID productId,
                     String productName,
                     String productCategory,
                     BillingCycle billingCycle,
                     int quantity) {
        super(id);
        Guard.againstNull(productId, "productId");
        Guard.againstNullOrBlank(productName, "productName");
        Guard.againstNullOrBlank(productCategory, "productCategory");
        Guard.againstNull(billingCycle, "billingCycle");
        Guard.againstOutOfRange(quantity, MIN_QUANTITY, MAX_QUANTITY, "quantity");

        this.productId = productId;
        this.productName = productName;
        this.productCategory = productCategory;
        this.billingCycle = billingCycle;
        this.quantity = quantity;
    }

    public static CartLine create(UUID productId,
                                  String productName,
                                  String productCategory,
                                  BillingCycle billingCycle,
                                  int quantity) {
        return new CartLine(UUID.randomUUID(), productId, productName, productCategory, billingCycle, quantity);
    }

    public static CartLine reconstitute(UUID id,
                                        UUID productId,
                                        String productName,
                                        String productCategory,
                                        BillingCycle billingCycle,
                                        int quantity) {
        Guard.againstNull(id, "id");
        return new CartLine(id, productId, productName, productCategory, billingCycle, quantity);
    }

    public CartLine withQuantity(int newQuantity) {
        return new CartLine(getId(), productId, productName, productCategory, billingCycle, newQuantity);
    }

    public CartLine withBillingCycle(BillingCycle newBillingCycle) {
        return new CartLine(getId(), productId, productName, productCategory, newBillingCycle, quantity);
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
}
