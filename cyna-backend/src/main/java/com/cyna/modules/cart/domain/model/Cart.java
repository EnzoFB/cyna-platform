package com.cyna.modules.cart.domain.model;

import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Cart extends AggregateRoot<UUID> {

    private final UUID userId;
    private final String guestToken;
    private final CartStatus status;
    private final List<CartLine> lines;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Cart(UUID id,
                 UUID userId,
                 String guestToken,
                 CartStatus status,
                 List<CartLine> lines,
                 Instant createdAt,
                 Instant updatedAt) {
        super(id);
        Guard.againstNull(id, "id");
        Guard.againstNull(status, "status");
        Guard.againstNull(lines, "lines");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        boolean hasUser = userId != null;
        boolean hasGuest = guestToken != null && !guestToken.isBlank();
        if (hasUser == hasGuest) {
            throw new IllegalArgumentException("Exactly one owner must be set: userId or guestToken");
        }

        this.userId = userId;
        this.guestToken = hasGuest ? guestToken.trim() : null;
        this.status = status;
        this.lines = List.copyOf(lines);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Cart createForUser(UUID userId) {
        Guard.againstNull(userId, "userId");
        Instant now = Instant.now();
        return new Cart(UUID.randomUUID(), userId, null, CartStatus.ACTIVE, List.of(), now, now);
    }

    public static Cart createForGuest(String guestToken) {
        Guard.againstNullOrBlank(guestToken, "guestToken");
        Instant now = Instant.now();
        return new Cart(UUID.randomUUID(), null, guestToken, CartStatus.ACTIVE, List.of(), now, now);
    }

    public static Cart reconstitute(UUID id,
                                    UUID userId,
                                    String guestToken,
                                    CartStatus status,
                                    List<CartLine> lines,
                                    Instant createdAt,
                                    Instant updatedAt) {
        return new Cart(id, userId, guestToken, status, lines, createdAt, updatedAt);
    }

    public Result<Cart> addOrMergeLine(UUID productId,
                                       String productName,
                                       String productCategory,
                                       BillingCycle billingCycle,
                                       int quantityToAdd) {
        if (status != CartStatus.ACTIVE) {
            return Result.failure("Only active carts can be modified");
        }

        Guard.againstNull(productId, "productId");
        Guard.againstNullOrBlank(productName, "productName");
        Guard.againstNullOrBlank(productCategory, "productCategory");
        Guard.againstNull(billingCycle, "billingCycle");
        if (!isQuantityInRange(quantityToAdd)) {
            return Result.failure("Quantity must be between 1 and 99");
        }

        List<CartLine> updated = new ArrayList<>(lines);
        Optional<CartLine> existing = findLineByProductAndCycle(productId, billingCycle);
        if (existing.isPresent()) {
            CartLine line = existing.get();
            int merged = line.getQuantity() + quantityToAdd;
            if (merged > CartLine.MAX_QUANTITY) {
                return Result.failure("Quantity must be between 1 and 99");
            }
            replaceById(updated, line.withQuantity(merged));
        } else {
            updated.add(CartLine.create(productId, productName, productCategory, billingCycle, quantityToAdd));
        }

        return Result.success(withLines(updated));
    }

    public Result<Cart> updateLineQuantity(UUID lineId, int quantity) {
        if (status != CartStatus.ACTIVE) {
            return Result.failure("Only active carts can be modified");
        }
        Guard.againstNull(lineId, "lineId");

        Optional<CartLine> existing = findLineById(lineId);
        if (existing.isEmpty()) {
            return Result.failure("Cart line not found: " + lineId);
        }
        if (!isQuantityInRange(quantity)) {
            return Result.failure("Quantity must be between 1 and 99");
        }

        List<CartLine> updated = new ArrayList<>(lines);
        replaceById(updated, existing.get().withQuantity(quantity));
        return Result.success(withLines(updated));
    }

    public Result<Cart> removeLine(UUID lineId) {
        if (status != CartStatus.ACTIVE) {
            return Result.failure("Only active carts can be modified");
        }
        Guard.againstNull(lineId, "lineId");

        List<CartLine> updated = lines.stream()
                .filter(line -> !line.getId().equals(lineId))
                .toList();

        if (updated.size() == lines.size()) {
            return Result.failure("Cart line not found: " + lineId);
        }

        return Result.success(withLines(updated));
    }

    public Result<Cart> changeLineBillingCycle(UUID lineId, BillingCycle newCycle) {
        if (status != CartStatus.ACTIVE) {
            return Result.failure("Only active carts can be modified");
        }
        Guard.againstNull(lineId, "lineId");
        Guard.againstNull(newCycle, "newCycle");

        Optional<CartLine> sourceOpt = findLineById(lineId);
        if (sourceOpt.isEmpty()) {
            return Result.failure("Cart line not found: " + lineId);
        }

        CartLine source = sourceOpt.get();
        if (source.getBillingCycle() == newCycle) {
            return Result.success(this);
        }

        List<CartLine> updated = new ArrayList<>(lines);
        Optional<CartLine> targetOpt = findLineByProductAndCycle(source.getProductId(), newCycle);

        if (targetOpt.isPresent()) {
            CartLine target = targetOpt.get();
            int mergedQuantity = target.getQuantity() + source.getQuantity();
            if (mergedQuantity > CartLine.MAX_QUANTITY) {
                return Result.failure("Quantity must be between 1 and 99");
            }
            updated.removeIf(line -> line.getId().equals(source.getId()));
            replaceById(updated, target.withQuantity(mergedQuantity));
            return Result.success(withLines(updated));
        }

        replaceById(updated, source.withBillingCycle(newCycle));
        return Result.success(withLines(updated));
    }

    public Result<Cart> mergeFrom(Cart otherCart) {
        Guard.againstNull(otherCart, "otherCart");
        if (status != CartStatus.ACTIVE) {
            return Result.failure("Only active carts can be modified");
        }
        if (otherCart.getStatus() != CartStatus.ACTIVE) {
            return Result.failure("Only active carts can be merged");
        }

        Cart current = this;
        for (CartLine line : otherCart.getLines()) {
            Result<Cart> merged = current.addOrMergeLine(
                    line.getProductId(),
                    line.getProductName(),
                    line.getProductCategory(),
                    line.getBillingCycle(),
                    line.getQuantity()
            );
            if (merged.isFailure()) {
                return merged;
            }
            current = merged.getValue();
        }

        return Result.success(current);
    }

    public Result<Cart> attachToUser(UUID userId) {
        if (status != CartStatus.ACTIVE) {
            return Result.failure("Only active carts can be modified");
        }
        Guard.againstNull(userId, "userId");
        if (guestToken == null) {
            return Result.failure("Cart is already attached to a user");
        }

        return Result.success(new Cart(
                getId(),
                userId,
                null,
                status,
                lines,
                createdAt,
                Instant.now()
        ));
    }

    public Result<Cart> markCheckedOut() {
        if (status == CartStatus.CHECKED_OUT) {
            return Result.failure("Cart is already checked out");
        }

        return Result.success(new Cart(
                getId(),
                userId,
                guestToken,
                CartStatus.CHECKED_OUT,
                lines,
                createdAt,
                Instant.now()
        ));
    }

    /**
     * Cart-side preview: HT subtotal and currency only. VAT is intentionally
     * not computed here — it is the payment module's job (Stripe Tax) at
     * checkout, based on the billing address and B2B status. Cart used to
     * advertise a flat 20% which lied as soon as the customer was outside FR.
     */
    public Result<CartTotals> calculateTotals(List<CartProductPricing> pricings) {
        Guard.againstNull(pricings, "pricings");

        Map<UUID, CartProductPricing> byProductId = new LinkedHashMap<>();
        for (CartProductPricing pricing : pricings) {
            byProductId.put(pricing.productId(), pricing);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = null;
        for (CartLine line : lines) {
            CartProductPricing pricing = byProductId.get(line.getProductId());
            if (pricing == null) {
                return Result.failure("Product pricing not found: " + line.getProductId());
            }
            if (!pricing.published()) {
                return Result.failure("Product is no longer available: " + line.getProductId());
            }

            BigDecimal unitPrice = line.getBillingCycle() == BillingCycle.MONTHLY
                    ? pricing.monthlyPrice()
                    : pricing.annualPrice();

            if (currency == null) {
                currency = pricing.currency();
            } else if (!currency.equals(pricing.currency())) {
                return Result.failure("Cart must use a single currency");
            }

            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(line.getQuantity())));
        }

        String effectiveCurrency = currency == null ? "EUR" : currency;
        return Result.success(new CartTotals(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                effectiveCurrency
        ));
    }

    private Cart withLines(List<CartLine> newLines) {
        return new Cart(
                getId(),
                userId,
                guestToken,
                status,
                newLines,
                createdAt,
                Instant.now()
        );
    }

    private Optional<CartLine> findLineById(UUID lineId) {
        return lines.stream().filter(line -> line.getId().equals(lineId)).findFirst();
    }

    private Optional<CartLine> findLineByProductAndCycle(UUID productId, BillingCycle cycle) {
        return lines.stream()
                .filter(line -> line.getProductId().equals(productId) && line.getBillingCycle() == cycle)
                .findFirst();
    }

    private void replaceById(List<CartLine> source, CartLine replacement) {
        for (int i = 0; i < source.size(); i++) {
            if (source.get(i).getId().equals(replacement.getId())) {
                source.set(i, replacement);
                return;
            }
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public String getGuestToken() {
        return guestToken;
    }

    public CartStatus getStatus() {
        return status;
    }

    public List<CartLine> getLines() {
        return lines;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private boolean isQuantityInRange(int quantity) {
        return quantity >= CartLine.MIN_QUANTITY && quantity <= CartLine.MAX_QUANTITY;
    }
}
