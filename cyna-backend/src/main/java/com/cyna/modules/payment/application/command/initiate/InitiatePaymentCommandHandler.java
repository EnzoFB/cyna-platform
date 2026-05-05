package com.cyna.modules.payment.application.command.initiate;

import com.cyna.modules.order.application.api.OrderPaymentView;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.application.model.PaymentInitiatedReadModel;
import com.cyna.modules.payment.domain.model.Payment;
import com.cyna.modules.payment.domain.model.PaymentStatus;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort.SubscriptionLineItem;
import com.cyna.modules.payment.domain.repository.PaymentRepository;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.user.application.api.UserQueryApi;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

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
        try {
            return transactionRunner.runReturning(() -> {

                // 1. Charger et valider la commande
                OrderPaymentView order = orderQueryApi
                        .findOrderForPayment(command.orderId(), command.userId())
                        .orElse(null);

                if (order == null) {
                    return Result.failure("ORDER_NOT_FOUND");
                }
                if (!"PENDING".equals(order.status())) {
                    return Result.failure("ORDER_NOT_PAYABLE");
                }

                // 2. Valider le cycle de facturation (toutes les lignes doivent avoir le même)
                List<String> billingCycles = order.lines().stream()
                        .map(OrderPaymentView.OrderLineView::billingCycle)
                        .distinct()
                        .toList();
                if (billingCycles.size() > 1) {
                    return Result.failure("MIXED_BILLING_CYCLES");
                }
                String billingCycle = billingCycles.get(0);

                // 3. Idempotence : retourner le paiement existant si PENDING
                var existing = paymentRepository.findByOrderId(command.orderId()).orElse(null);
                if (existing != null && existing.getStatus() == PaymentStatus.PENDING
                        && existing.getStripeClientSecret() != null) {
                    return Result.success(toReadModel(existing, order));
                }

                // 4. Récupérer les infos utilisateur pour Stripe
                var user = userQueryApi.findUserForPayment(command.userId()).orElse(null);
                if (user == null) {
                    return Result.failure("USER_NOT_FOUND");
                }

                // 5. Récupérer le client Stripe existant
                String existingCustomerId = stripeCustomerRepository
                        .findStripeCustomerIdByUserId(command.userId())
                        .orElse(null);

                // 6. Construire les lignes d'abonnement
                List<SubscriptionLineItem> lineItems = order.lines().stream()
                        .map(line -> new SubscriptionLineItem(
                                line.productId(),
                                line.productName(),
                                line.unitPrice(),
                                line.quantity()
                        ))
                        .toList();

                // 7. Créer l'abonnement Stripe
                var subResult = paymentGateway.createSubscription(
                        command.orderId(),
                        existingCustomerId,
                        user.email(),
                        user.firstName() + " " + user.lastName(),
                        lineItems,
                        order.currency(),
                        billingCycle
                );

                // 8. Persister le mapping client Stripe (upsert si changé)
                if (!subResult.stripeCustomerId().equals(existingCustomerId)) {
                    stripeCustomerRepository.save(command.userId(), subResult.stripeCustomerId());
                }

                // 9. Créer et persister l'entité Payment
                Money amount = Money.of(order.totalAmount(), order.currency());
                Payment payment = Payment
                        .create(UUID.randomUUID(), command.orderId(), command.userId(), amount)
                        .assignStripeSubscription(
                                subResult.paymentIntentId(),
                                subResult.clientSecret(),
                                subResult.subscriptionId(),
                                subResult.scheduleId()
                        );

                paymentRepository.save(payment);
                eventPublisher.publishAll(payment.getDomainEvents());
                payment.clearDomainEvents();

                return Result.success(toReadModel(payment, order));
            });
        } catch (PaymentGatewayException e) {
            log.error("Stripe subscription creation failed for order {}: {}", command.orderId(), e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }
    }

    private PaymentInitiatedReadModel toReadModel(Payment payment, OrderPaymentView order) {
        return new PaymentInitiatedReadModel(
                payment.getId(),
                payment.getOrderId(),
                payment.getStripeClientSecret(),
                order.totalAmount(),
                order.currency()
        );
    }
}
