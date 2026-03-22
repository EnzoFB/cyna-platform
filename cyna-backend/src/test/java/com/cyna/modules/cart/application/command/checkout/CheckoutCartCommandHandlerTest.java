package com.cyna.modules.cart.application.command.checkout;

import com.cyna.modules.cart.application.model.CartTotalsReadModel;
import com.cyna.modules.cart.application.service.CartAccessService;
import com.cyna.modules.cart.application.service.CartReadModelService;
import com.cyna.modules.cart.domain.model.BillingCycle;
import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.modules.cart.domain.model.CartStatus;
import com.cyna.modules.cart.domain.repository.CartRepository;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutCartCommandHandlerTest {

    @Mock
    private CartAccessService cartAccessService;

    @Mock
    private CartReadModelService cartReadModelService;

    @Mock
    private CartRepository cartRepository;

    private CheckoutCartCommandHandler handler;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override
        public void run(Runnable action) {
            action.run();
        }

        @Override
        public <T> T runReturning(Supplier<T> action) {
            return action.get();
        }
    };

    @BeforeEach
    void setUp() {
        handler = new CheckoutCartCommandHandler(
                cartAccessService,
                cartReadModelService,
                cartRepository,
                transactionRunner
        );
    }

    @Test
    void should_fail_when_user_is_not_authenticated() {
        Result<?> result = handler.handle(new CheckoutCartCommand(null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Authenticated user is required");
        verify(cartAccessService, never()).getRequiredActiveCart(null, null);
    }

    @Test
    void should_fail_with_conflict_message_when_latest_cart_is_already_checked_out() {
        UUID userId = UUID.randomUUID();
        Cart checkedOutCart = Cart.createForUser(userId).markCheckedOut().getValue();

        when(cartAccessService.getRequiredActiveCart(userId, null))
                .thenReturn(Result.failure("Active cart not found"));
        when(cartRepository.findLatestByUserId(userId)).thenReturn(Optional.of(checkedOutCart));

        Result<?> result = handler.handle(new CheckoutCartCommand(userId));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Cart is already checked out");
        verify(cartRepository).findLatestByUserId(userId);
        verify(cartReadModelService, never()).calculateTotals(checkedOutCart);
    }

    @Test
    void should_fail_when_cart_is_empty() {
        UUID userId = UUID.randomUUID();
        Cart emptyCart = Cart.createForUser(userId);

        when(cartAccessService.getRequiredActiveCart(userId, null)).thenReturn(Result.success(emptyCart));

        Result<?> result = handler.handle(new CheckoutCartCommand(userId));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("Cart is empty");
        verify(cartReadModelService, never()).calculateTotals(emptyCart);
    }

    @Test
    void should_checkout_cart_successfully() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Cart activeCart = Cart.createForUser(userId)
                .addOrMergeLine(productId, "Cyna EDR 1", "EDR", BillingCycle.MONTHLY, 2)
                .getValue();

        CartTotalsReadModel totals = new CartTotalsReadModel(
                BigDecimal.valueOf(600.00),
                BigDecimal.valueOf(120.00),
                BigDecimal.valueOf(720.00),
                "EUR"
        );

        when(cartAccessService.getRequiredActiveCart(userId, null)).thenReturn(Result.success(activeCart));
        when(cartReadModelService.calculateTotals(activeCart)).thenReturn(Result.success(totals));

        Result<?> result = handler.handle(new CheckoutCartCommand(userId));

        assertThat(result.isSuccess()).isTrue();
        verify(cartReadModelService).calculateTotals(activeCart);

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartAccessService).save(cartCaptor.capture());
        assertThat(cartCaptor.getValue().getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
    }
}
