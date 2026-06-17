package com.cyna.modules.order.application.api;

import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.domain.Result;

import java.util.List;
import java.util.UUID;

public interface OrderCommandApi {

    Result<Void> markOrderAsPaid(UUID orderId, String lang);

    /**
     * Creates an order from a checked-out cart, synchronously, in the caller's
     * transaction. Returns the new order id on success. Used by the cart module
     * to chain cart checkout → order creation atomically (same seam as
     * {@code payment → subscription} in {@code FinalizePaymentCommandHandler}).
     *
     * <p>{@code billingAddress} may be {@code null} — at cart checkout we don't
     * always have it yet (the customer enters it at the payment step). Stripe
     * Tax derives the destination jurisdiction from the PaymentMethod's billing
     * details later in {@code updateCustomerTaxLocation}, so the Order can be
     * persisted without an address. Future iterations of the cart can collect
     * the address upfront and pass it here.
     */
    Result<UUID> createOrderFromCart(UUID userId,
                                     List<CartLineRequest> lines,
                                     BillingAddressRequest billingAddress);

    record CartLineRequest(UUID productId, BillingCycle billingCycle, int quantity) {
    }

    record BillingAddressRequest(String line1, String city, String zipCode, String countryCode) {
    }
}
