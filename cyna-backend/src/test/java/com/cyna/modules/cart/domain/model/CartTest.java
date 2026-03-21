package com.cyna.modules.cart.domain.model;

import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

    @Test
    void should_create_guest_cart_with_active_status() {
        Cart cart = Cart.createForGuest("guest-123");

        assertThat(cart.getId()).isNotNull();
        assertThat(cart.getGuestToken()).isEqualTo("guest-123");
        assertThat(cart.getUserId()).isNull();
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cart.getLines()).isEmpty();
    }

    @Test
    void should_merge_quantity_when_same_product_and_cycle_added() {
        UUID productId = UUID.randomUUID();
        Cart cart = Cart.createForGuest("guest-123");

        Result<Cart> firstAdd = cart.addOrMergeLine(productId, "Cyna EDR 1", "EDR", BillingCycle.MONTHLY, 1);
        Result<Cart> secondAdd = firstAdd.getValue().addOrMergeLine(productId, "Cyna EDR 1", "EDR", BillingCycle.MONTHLY, 2);

        assertThat(secondAdd.isSuccess()).isTrue();
        assertThat(secondAdd.getValue().getLines()).hasSize(1);
        assertThat(secondAdd.getValue().getLines().getFirst().getQuantity()).isEqualTo(3);
    }

    @Test
    void should_fail_when_merged_quantity_exceeds_max() {
        UUID productId = UUID.randomUUID();
        Cart cart = Cart.createForGuest("guest-123");

        Result<Cart> firstAdd = cart.addOrMergeLine(productId, "Cyna EDR 1", "EDR", BillingCycle.MONTHLY, 99);
        Result<Cart> secondAdd = firstAdd.getValue().addOrMergeLine(productId, "Cyna EDR 1", "EDR", BillingCycle.MONTHLY, 1);

        assertThat(secondAdd.isFailure()).isTrue();
        assertThat(secondAdd.getError()).isEqualTo("Quantity must be between 1 and 99");
    }

    @Test
    void should_merge_lines_when_billing_cycle_changes_to_existing_target_cycle() {
        UUID productId = UUID.randomUUID();
        Cart cart = Cart.createForGuest("guest-123");

        Result<Cart> withMonthly = cart.addOrMergeLine(productId, "Cyna EDR 1", "EDR", BillingCycle.MONTHLY, 2);
        Result<Cart> withAnnual = withMonthly.getValue()
                .addOrMergeLine(productId, "Cyna EDR 1", "EDR", BillingCycle.ANNUAL, 1);

        UUID monthlyLineId = withAnnual.getValue().getLines().stream()
                .filter(line -> line.getBillingCycle() == BillingCycle.MONTHLY)
                .findFirst()
                .orElseThrow()
                .getId();

        Result<Cart> changed = withAnnual.getValue().changeLineBillingCycle(monthlyLineId, BillingCycle.ANNUAL);

        assertThat(changed.isSuccess()).isTrue();
        assertThat(changed.getValue().getLines()).hasSize(1);
        CartLine line = changed.getValue().getLines().getFirst();
        assertThat(line.getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
        assertThat(line.getQuantity()).isEqualTo(3);
    }

    @Test
    void should_calculate_totals_with_vat_from_live_prices() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        Cart cart = Cart.createForGuest("guest-123")
                .addOrMergeLine(p1, "Cyna EDR 1", "EDR", BillingCycle.MONTHLY, 1)
                .getValue()
                .addOrMergeLine(p2, "Cyna EDR 2", "EDR", BillingCycle.MONTHLY, 1)
                .getValue();

        List<CartProductPricing> pricings = List.of(
                new CartProductPricing(p1, BigDecimal.valueOf(300), BigDecimal.valueOf(3000), "EUR", true),
                new CartProductPricing(p2, BigDecimal.valueOf(500), BigDecimal.valueOf(5000), "EUR", true)
        );

        Result<CartTotals> totals = cart.calculateTotals(pricings, BigDecimal.valueOf(0.20));

        assertThat(totals.isSuccess()).isTrue();
        assertThat(totals.getValue().subtotalHt()).isEqualByComparingTo("800.00");
        assertThat(totals.getValue().vatAmount()).isEqualByComparingTo("160.00");
        assertThat(totals.getValue().totalTtc()).isEqualByComparingTo("960.00");
        assertThat(totals.getValue().currency()).isEqualTo("EUR");
    }

    @Test
    void should_block_totals_when_product_is_unpublished() {
        UUID productId = UUID.randomUUID();
        Cart cart = Cart.createForGuest("guest-123")
                .addOrMergeLine(productId, "Cyna EDR 1", "EDR", BillingCycle.MONTHLY, 1)
                .getValue();

        Result<CartTotals> totals = cart.calculateTotals(
                List.of(new CartProductPricing(
                        productId,
                        BigDecimal.valueOf(300),
                        BigDecimal.valueOf(3000),
                        "EUR",
                        false
                )),
                BigDecimal.valueOf(0.20)
        );

        assertThat(totals.isFailure()).isTrue();
        assertThat(totals.getError()).startsWith("Product is no longer available:");
    }

    @Test
    void should_attach_guest_cart_to_user() {
        Cart guestCart = Cart.createForGuest("guest-123");

        Result<Cart> attached = guestCart.attachToUser(UUID.randomUUID());

        assertThat(attached.isSuccess()).isTrue();
        assertThat(attached.getValue().getGuestToken()).isNull();
        assertThat(attached.getValue().getUserId()).isNotNull();
    }
}
