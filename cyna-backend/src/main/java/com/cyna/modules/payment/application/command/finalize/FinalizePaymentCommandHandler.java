package com.cyna.modules.payment.application.command.finalize;

import com.cyna.modules.order.application.api.OrderCommandApi;
import com.cyna.modules.order.application.api.OrderPaymentView;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.payment.application.model.PaymentFinalizedReadModel;
import com.cyna.modules.payment.domain.model.Payment;
import com.cyna.modules.payment.domain.model.PaymentStatus;
import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.PaymentRepository;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi;
import com.cyna.modules.subscription.application.api.SubscriptionCommandApi.CreatedSubscriptionView;
import com.cyna.modules.subscription.application.api.SubscriptionPaymentPayload;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
public class FinalizePaymentCommandHandler
        implements CommandHandler<FinalizePaymentCommand, PaymentFinalizedReadModel> {

    private static final Logger log = LoggerFactory.getLogger(FinalizePaymentCommandHandler.class);

    private final OrderQueryApi orderQueryApi;
    private final OrderCommandApi orderCommandApi;
    private final PaymentRepository paymentRepository;
    private final StripeCustomerRepository stripeCustomerRepository;
    private final PaymentGatewayPort paymentGateway;
    private final SubscriptionCommandApi subscriptionCommandApi;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public FinalizePaymentCommandHandler(OrderQueryApi orderQueryApi,
            OrderCommandApi orderCommandApi,
            PaymentRepository paymentRepository,
            StripeCustomerRepository stripeCustomerRepository,
            PaymentGatewayPort paymentGateway,
            SubscriptionCommandApi subscriptionCommandApi,
            DomainEventPublisher eventPublisher,
            TransactionRunner transactionRunner) {
        this.orderQueryApi = orderQueryApi;
        this.orderCommandApi = orderCommandApi;
        this.paymentRepository = paymentRepository;
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.paymentGateway = paymentGateway;
        this.subscriptionCommandApi = subscriptionCommandApi;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<PaymentFinalizedReadModel> handle(FinalizePaymentCommand command) {
        // 1. Pre-flight: load order, payment, customer mapping outside any transaction.
        OrderPaymentView order = orderQueryApi
                .findOrderForPayment(command.orderId(), command.userId())
                .orElse(null);

        if (order == null)
            return Result.failure("ORDER_NOT_FOUND");

        Payment payment = paymentRepository.findByOrderId(command.orderId()).orElse(null);

        if (payment == null)
            return Result.failure("PAYMENT_NOT_INITIATED");

        if (!payment.getUserId().equals(command.userId()))
            return Result.failure("Access denied");

        if (payment.getStatus() == PaymentStatus.SUCCEEDED)
            return Result.success(buildIdempotentResponse(payment, order));

        // PENDING = first attempt. FAILED = a previous attempt was declined and
        // the customer is retrying (typically with another card). Both are
        // finalizable; only SUCCEEDED (handled above) and REFUNDED are terminal.
        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.FAILED)
            return Result.failure("PAYMENT_NOT_FINALIZABLE");


        String stripeCustomerId = stripeCustomerRepository
                .findStripeCustomerIdByUserId(command.userId())
                .orElse(null);
        if (stripeCustomerId == null)
            return Result.failure("NO_STRIPE_CUSTOMER");


        // 2. For each OrderLine, create the matching Stripe Subscription. Each call
        // is idempotent on Stripe's side (idempotency-key = "cyna-line-<lineId>"),
        // so a retry of the whole finalize is safe.
        List<CreatedLine> createdLines = new ArrayList<>();
        try {
            for (OrderPaymentView.OrderLineView line : order.lines()) {
                var stripeResult = paymentGateway.createSubscriptionForLine(
                        stripeCustomerId,
                        command.paymentMethodId(),
                        order.id(),
                        line.id(),
                        line.productId(),
                        line.productName(),
                        line.unitPrice(),
                        line.quantity(),
                        order.currency(),
                        line.billingCycle());
                createdLines.add(new CreatedLine(line, stripeResult));
            }
        } catch (PaymentGatewayException e) {
            // Partial failure: some lines already created at Stripe, others not. We do
            // NOT roll back the Stripe side — those subs exist and have their own
            // lifecycle. The customer can resume by retrying finalize (idempotency
            // keys make the previously-created subs return as-is). Local DB is
            // untouched at this point so the next retry restarts cleanly.
            log.error("[finalize] Stripe subscription creation failed for order {}: {}",
                    command.orderId(), e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }

        // 2b. Payment-result gate. Stripe created every Subscription with
        // ALLOW_INCOMPLETE, so getting a Subscription object back does NOT mean
        // the first invoice was charged. Only `active`/`trialing` means the
        // money actually moved; anything else (`incomplete`,
        // `incomplete_expired`, `past_due`, `unpaid`) means the off-session
        // charge was declined (insufficient funds, 3DS required, etc.).
        //
        // A single PaymentMethod is used for every line, so charges don't split
        // — they all settle or all fail together. We therefore treat the order
        // atomically: if any line did not settle we abort the WHOLE order —
        // immediately cancel every Subscription we just created (declined subs
        // were never charged, so there is nothing to refund), mark the Payment
        // FAILED so the customer can retry with another card, and surface
        // PAYMENT_DECLINED. The Order is never marked paid and no local
        // Subscription is created in this branch.
        boolean allSettled = createdLines.stream()
                .allMatch(cl -> isSettled(cl.stripeResult().status()));

        if (!allSettled) {
            for (CreatedLine cl : createdLines) {
                try {
                    paymentGateway.cancelSubscriptionNow(cl.stripeResult().stripeSubscriptionId());
                } catch (PaymentGatewayException e) {
                    // Best-effort rollback: a failed cancel must not mask the
                    // decline we are already reporting. An un-cancelled
                    // `incomplete` sub auto-expires at Stripe within ~23h.
                    log.warn("[finalize] Could not roll back Stripe subscription {} "
                                    + "after declined payment for order {}: {}",
                            cl.stripeResult().stripeSubscriptionId(),
                            command.orderId(), e.getMessage());
                }
            }

            // Mark FAILED only from PENDING. If the Payment is already FAILED
            // (the customer retried and was declined again) this is a no-op —
            // markFailed() would reject the FAILED→FAILED transition anyway.
            if (payment.getStatus() == PaymentStatus.PENDING) {
                transactionRunner.run(() -> {
                    Result<Payment> failedResult = payment.markFailed();
                    if (failedResult.isSuccess()) {
                        Payment failed = failedResult.getValue();
                        paymentRepository.save(failed);
                        eventPublisher.publishAll(failed.getDomainEvents());
                        failed.clearDomainEvents();
                    }
                });
            }

            long declinedCount = createdLines.stream()
                    .filter(cl -> !isSettled(cl.stripeResult().status()))
                    .count();
            log.info("[finalize] Payment declined for order {} — {}/{} line(s) not settled; "
                            + "all created subscriptions rolled back",
                    command.orderId(), declinedCount, createdLines.size());
            return Result.failure("PAYMENT_DECLINED");
        }

        // 3. Persist everything: local Subscriptions, Order PAID, Payment SUCCEEDED.
        // All-or-nothing — if any DB write fails, the whole transaction rolls
        // back and the user can retry. Stripe side stays as-is and is reused
        // via idempotency keys on the next call.
        return transactionRunner.runReturning(() -> {
            List<PaymentFinalizedReadModel.FinalizedLine> finalized = new ArrayList<>();

            Instant startAt = Instant.now();
            for (CreatedLine cl : createdLines) {
                BillingCycle cycle = BillingCycle.valueOf(cl.line().billingCycle());
                Instant endAt = endDateFor(startAt, cycle);

                SubscriptionPaymentPayload payload = new SubscriptionPaymentPayload(
                        command.userId(),
                        order.id(),
                        cl.line().id(),
                        cl.line().productId(),
                        cl.line().productName(),
                        cl.line().productCategory(),
                        cycle,
                        cl.line().quantity(),
                        cl.line().unitPrice(),
                        order.currency(),
                        startAt,
                        endAt,
                        endAt,
                        cl.stripeResult().stripeSubscriptionId(),
                        null);
                Result<CreatedSubscriptionView> created = subscriptionCommandApi.createFromPayment(payload);
                if (created.isFailure()) {
                    log.error("[finalize] Failed to create local subscription for line {}: {}",
                            cl.line().id(), created.getError());
                    throw new IllegalStateException(
                            "Local subscription creation failed for line " + cl.line().id()
                                    + ": " + created.getError());
                }

                finalized.add(new PaymentFinalizedReadModel.FinalizedLine(
                        cl.line().id(),
                        created.getValue().subscriptionId(),
                        cl.stripeResult().stripeSubscriptionId(),
                        cl.stripeResult().status()));
            }

            // Order PAID — idempotent if already PAID (the API rejects, we ignore).
            orderCommandApi.markOrderAsPaid(order.id());

            Result<Payment> succeededResult = payment.markSucceeded();
            if (succeededResult.isFailure()) {
                throw new IllegalStateException(
                        "Cannot mark payment succeeded: " + succeededResult.getError());
            }
            Payment succeeded = succeededResult.getValue();
            paymentRepository.save(succeeded);
            eventPublisher.publishAll(succeeded.getDomainEvents());
            succeeded.clearDomainEvents();

            return Result.success(new PaymentFinalizedReadModel(
                    succeeded.getId(),
                    succeeded.getOrderId(),
                    finalized));
        });
    }

    /**
     * A Stripe Subscription whose first invoice was actually paid. Everything
     * else (`incomplete`, `incomplete_expired`, `past_due`, `unpaid`,
     * `canceled`) means the off-session charge did not go through and the
     * checkout must not be treated as complete.
     */
    private static boolean isSettled(String stripeSubscriptionStatus) {
        return "active".equals(stripeSubscriptionStatus)
                || "trialing".equals(stripeSubscriptionStatus);
    }

    private static Instant endDateFor(Instant startAt, BillingCycle billingCycle) {
        LocalDate startDate = startAt.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = billingCycle == BillingCycle.MONTHLY
                ? startDate.plusMonths(1)
                : startDate.plusYears(1);
        return endDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static PaymentFinalizedReadModel buildIdempotentResponse(Payment payment, OrderPaymentView order) {
        // For an already-SUCCEEDED Payment, return an empty per-line breakdown —
        // the front can read the actual subscription states via
        // GET /api/v1/subscriptions (already paginated). This keeps the idempotent
        // response cheap and avoids the extra fetch when the data is stale anyway.
        return new PaymentFinalizedReadModel(payment.getId(), order.id(), List.of());
    }

    private record CreatedLine(
            OrderPaymentView.OrderLineView line,
            PaymentGatewayPort.SubscriptionForLineResult stripeResult) {
    }
}
