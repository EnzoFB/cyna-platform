package com.cyna.modules.cart.application.service;

import com.cyna.modules.cart.application.model.CartLineReadModel;
import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.modules.cart.application.model.CartTotalsReadModel;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.modules.cart.domain.model.CartProductPricing;
import com.cyna.modules.cart.domain.model.CartStatus;
import com.cyna.modules.cart.domain.model.CartTotals;
import com.cyna.modules.product.application.api.ProductInfo;
import com.cyna.modules.product.application.api.ProductQueryApi;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Assembles cart read models from the {@code Cart} aggregate, enriching lines
 * with up-to-date product info via {@link ProductQueryApi}. Cart totals are
 * <em>HT only</em>; VAT is computed downstream by the payment module via
 * Stripe Tax at checkout (it depends on the billing country and B2B status,
 * which the cart does not know).
 */
@Component
public class CartReadModelService {

    private static final String DEFAULT_CURRENCY = "EUR";

    private final ProductQueryApi productQueryApi;

    public CartReadModelService(ProductQueryApi productQueryApi) {
        this.productQueryApi = productQueryApi;
    }

    public Result<ProductInfo> getPurchasableProduct(UUID productId) {
        return productQueryApi.getById(productId)
                .map(product -> {
                    if (!product.isPublished()) {
                        return Result.<ProductInfo>failure("Product is no longer available: " + productId);
                    }
                    return Result.success(product);
                })
                .orElse(Result.failure("Product not found: " + productId));
    }

    public Result<CartTotalsReadModel> calculateTotals(Cart cart) {
        Result<CartTotals> totalsResult = cart.calculateTotals(buildPricingInputs(cart));
        return totalsResult.map(totals -> new CartTotalsReadModel(
                totals.subtotalHt(),
                totals.currency()
        ));
    }

    public CartReadModel toReadModel(Cart cart) {
        Map<UUID, ProductInfo> productsById = loadProducts(cart);
        List<CartLineReadModel> lineReadModels = new ArrayList<>();
        for (var line : cart.getLines()) {
            ProductInfo product = productsById.get(line.getProductId());
            boolean available = product != null && product.isPublished();

            BigDecimal unitPrice = null;
            String currency = DEFAULT_CURRENCY;
            if (product != null) {
                unitPrice = line.getBillingCycle() == BillingCycle.MONTHLY
                        ? product.monthlyPrice()
                        : product.annualPrice();
                currency = product.currency();
            }

            lineReadModels.add(new CartLineReadModel(
                    line.getId(),
                    line.getProductId(),
                    line.getProductName(),
                    line.getProductCategory(),
                    line.getBillingCycle().name(),
                    line.getQuantity(),
                    unitPrice,
                    currency,
                    available
            ));
        }

        List<String> errors = new ArrayList<>();
        CartTotalsReadModel totals = null;
        Result<CartTotalsReadModel> totalsResult = calculateTotals(cart);
        if (totalsResult.isSuccess()) {
            totals = totalsResult.getValue();
        } else {
            errors.add(totalsResult.getError());
        }
        if (cart.getLines().isEmpty()) {
            errors.add("Cart is empty");
            if (totals == null) {
                totals = new CartTotalsReadModel(BigDecimal.ZERO, DEFAULT_CURRENCY);
            }
        }

        boolean checkoutAllowed = cart.getStatus() == CartStatus.ACTIVE
                && !cart.getLines().isEmpty()
                && errors.isEmpty();

        return new CartReadModel(
                cart.getId(),
                cart.getUserId(),
                cart.getStatus().name(),
                lineReadModels,
                totals,
                checkoutAllowed,
                List.copyOf(errors)
        );
    }

    private List<CartProductPricing> buildPricingInputs(Cart cart) {
        Map<UUID, ProductInfo> productsById = loadProducts(cart);
        return cart.getLines().stream().map(line -> {
            ProductInfo product = productsById.get(line.getProductId());
            if (product == null) {
                return new CartProductPricing(
                        line.getProductId(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        DEFAULT_CURRENCY,
                        false
                );
            }
            return new CartProductPricing(
                    line.getProductId(),
                    product.monthlyPrice(),
                    product.annualPrice(),
                    product.currency(),
                    product.isPublished()
            );
        }).toList();
    }

    private Map<UUID, ProductInfo> loadProducts(Cart cart) {
        List<UUID> productIds = cart.getLines().stream()
                .map(line -> line.getProductId())
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, ProductInfo> byProductId = new LinkedHashMap<>();
        for (ProductInfo product : productQueryApi.getByIds(productIds)) {
            byProductId.put(product.id(), product);
        }
        return byProductId;
    }
}
