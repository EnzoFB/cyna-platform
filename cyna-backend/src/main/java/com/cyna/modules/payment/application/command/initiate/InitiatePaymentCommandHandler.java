package com.cyna.modules.payment.application.command.initiate;

import com.cyna.modules.order.application.api.OrderPaymentView;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.application.model.PaymentInitiatedReadModel;
import com.cyna.modules.payment.domain.model.Payment;
import com.cyna.modules.payment.domain.model.PaymentStatus;
import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.PaymentRepository;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.modules.user.application.api.UserQueryApi;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Step 1 of the V14 multi-product checkout flow. Creates a Stripe SetupIntent
 * the frontend will use to collect a card via PaymentElement. The actual
 * Subscription creation happens later in {@code FinalizePaymentCommandHandler},
 * once we have a PaymentMethod id back from Stripe.
 *
 * <p>Allows mixed cycles (monthly + annual lines in the same cart) — this is
 * the central reason for moving away from the old one-Stripe-sub-per-order
 * flow, which forced a single billing cycle for the whole order.
 */
@Component
public class InitiatePaymentCommandHandler
        implements CommandHandler<InitiatePaymentCommand, PaymentInitiatedReadModel> {

    private static final Logger log = LoggerFactory.getLogger(InitiatePaymentCommandHandler.class);

    private final OrderQueryApi orderQueryApi;
    private final UserQueryApi userQueryApi;
    private final PaymentRepository paymentRepository;
    private final StripeCustomerRepository stripeCustomerRepository;
    private final PaymentGatewayPort paymentGateway;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public InitiatePaymentCommandHandler(OrderQueryApi orderQueryApi,
                                         UserQueryApi userQueryApi,
                                         PaymentRepository paymentRepository,
                                         StripeCustomerRepository stripeCustomerRepository,
                                         PaymentGatewayPort paymentGateway,
                                         DomainEventPublisher eventPublisher,
                                         TransactionRunner transactionRunner) {
        this.orderQueryApi = orderQueryApi;
        this.userQueryApi = userQueryApi;
        this.paymentRepository = paymentRepository;
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<PaymentInitiatedReadModel> handle(InitiatePaymentCommand command) {
        // 1. Load and validate the order outside any transaction.
        OrderPaymentView order = orderQueryApi
                .findOrderForPayment(command.orderId(), command.userId())
                .orElse(null);
        if (order == null) {
            return Result.failure("ORDER_NOT_FOUND");
        }
        if (!"PENDING".equals(order.status())) {
            return Result.failure("ORDER_NOT_PAYABLE");
        }

        // 2. Idempotency: existing PENDING Payment with a SetupIntent → reuse it.
        Payment existing = paymentRepository.findByOrderId(command.orderId()).orElse(null);
        if (existing != null
                && existing.getStatus() == PaymentStatus.PENDING
                && existing.getStripeSetupIntentClientSecret() != null) {
            return Result.success(toReadModel(existing, order));
        }

        // 3. Load user, resolve (or create) the Stripe Customer.
        var user = userQueryApi.findUserForPayment(command.userId()).orElse(null);
        if (user == null) {
            return Result.failure("USER_NOT_FOUND");
        }

        String customerId;
        try {
            customerId = stripeCustomerRepository
                    .findStripeCustomerIdByUserId(command.userId())
                    .orElseGet(() -> paymentGateway.createCustomerForUser(
                            user.email(), user.firstName() + " " + user.lastName()));
        } catch (PaymentGatewayException e) {
            log.error("[initiate] Stripe customer creation failed: {}", e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }

        // 4. Create the SetupIntent (outside the DB transaction — remote call).
        PaymentGatewayPort.SetupIntentResult setup;
        try {
            setup = paymentGateway.createSetupIntent(customerId);
        } catch (PaymentGatewayException e) {
            log.error("[initiate] Stripe SetupIntent creation failed for order {}: {}",
                    command.orderId(), e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }

        // 5. Persist mapping + Payment record in a single short transaction.
        return transactionRunner.runReturning(() -> {
            if (stripeCustomerRepository.findStripeCustomerIdByUserId(command.userId()).isEmpty()) {
                stripeCustomerRepository.save(command.userId(), customerId);
            }

            // HT amount stored on the Payment aggregate. The actual amount charged
            // by Stripe (HT + automatic_tax VAT) lives on the Stripe invoice.
            Money amount = Money.of(order.subtotalHt(), order.currency());
            Payment payment = (existing != null ? existing : Payment.create(
                    UUID.randomUUID(), command.orderId(), command.userId(), amount))
                    .assignSetupIntent(setup.setupIntentId(), setup.clientSecret());

            paymentRepository.save(payment);
            eventPublisher.publishAll(payment.getDomainEvents());
            payment.clearDomainEvents();

            return Result.success(toReadModel(payment, order));
        });
    }

    private PaymentInitiatedReadModel toReadModel(Payment payment, OrderPaymentView order) {
        return new PaymentInitiatedReadModel(
                payment.getId(),
                payment.getOrderId(),
                payment.getStripeSetupIntentClientSecret(),
                order.subtotalHt(),
                order.currency()
        );
    }
}
