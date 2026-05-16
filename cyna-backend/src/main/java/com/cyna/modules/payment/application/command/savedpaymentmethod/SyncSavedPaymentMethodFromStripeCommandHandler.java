package com.cyna.modules.payment.application.command.savedpaymentmethod;

import com.cyna.modules.payment.domain.model.SavedPaymentMethod;
import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.SavedPaymentMethodRepository;
import com.cyna.modules.payment.domain.repository.StripeCustomerRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class SyncSavedPaymentMethodFromStripeCommandHandler
        implements CommandHandler<SyncSavedPaymentMethodFromStripeCommand, Void> {

    private static final Logger log = LoggerFactory.getLogger(SyncSavedPaymentMethodFromStripeCommandHandler.class);

    static final String EVT_ATTACHED = "payment_method.attached";
    static final String EVT_DETACHED = "payment_method.detached";
    static final String EVT_UPDATED  = "payment_method.automatically_updated";

    private final SavedPaymentMethodRepository repository;
    private final StripeCustomerRepository stripeCustomerRepository;
    private final PaymentGatewayPort paymentGateway;

    public SyncSavedPaymentMethodFromStripeCommandHandler(
            SavedPaymentMethodRepository repository,
            StripeCustomerRepository stripeCustomerRepository,
            PaymentGatewayPort paymentGateway) {
        this.repository = repository;
        this.stripeCustomerRepository = stripeCustomerRepository;
        this.paymentGateway = paymentGateway;
    }

    @Override
    @Transactional
    public Result<Void> handle(SyncSavedPaymentMethodFromStripeCommand command) {
        return switch (command.eventType()) {
            case EVT_DETACHED -> handleDetached(command);
            case EVT_ATTACHED -> handleAttached(command);
            case EVT_UPDATED  -> handleAutoUpdated(command);
            default -> Result.success();
        };
    }

    /**
     * Stripe-side detach: the customer removed a card via the Stripe Customer
     * Portal, or an admin did via the Stripe Dashboard. We mirror the change
     * by deleting the local row. Idempotent: silent if no local row exists.
     */
    private Result<Void> handleDetached(SyncSavedPaymentMethodFromStripeCommand command) {
        repository.deleteByStripePaymentMethodId(command.stripePaymentMethodId());
        return Result.success();
    }

    /**
     * Stripe-side attach: typically the customer added a card via the Stripe
     * Customer Portal. Our own "Add card" flow already creates the local row
     * via {@link SavePaymentMethodCommandHandler}, so we no-op when it's
     * already there.
     */
    private Result<Void> handleAttached(SyncSavedPaymentMethodFromStripeCommand command) {
        if (repository.findByStripePaymentMethodId(command.stripePaymentMethodId()).isPresent()) {
            return Result.success();
        }
        UUID userId = stripeCustomerRepository
                .findUserIdByStripeCustomerId(command.stripeCustomerId())
                .orElse(null);
        if (userId == null) {
            // Webhook arrived for a customer we don't know — orphan event,
            // safe to ignore (likely a Stripe test environment crossover).
            log.warn("[sync-pm] attached event for unknown stripe customer {}",
                    command.stripeCustomerId());
            return Result.success();
        }

        PaymentGatewayPort.SavedPaymentMethodDetails details;
        try {
            details = paymentGateway.retrievePaymentMethodDetails(command.stripePaymentMethodId());
        } catch (PaymentGatewayException e) {
            log.error("[sync-pm] Stripe retrieve failed for {}: {}",
                    command.stripePaymentMethodId(), e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }

        boolean isFirst = repository.countByUserId(userId) == 0;
        SavedPaymentMethod created = SavedPaymentMethod.create(
                userId,
                command.stripePaymentMethodId(),
                details.brand(),
                details.last4(),
                details.expMonth(),
                details.expYear(),
                details.holderName(),
                isFirst
        );
        repository.save(created);
        return Result.success();
    }

    /**
     * Stripe's Card Account Updater (or a manual edit in the dashboard) changed
     * the card. We refresh the display metadata on the existing local row so
     * the user sees correct last4/expiry. Identity (id, userId, isDefault)
     * stays the same — Stripe keeps the same pm_xxx across these updates.
     */
    private Result<Void> handleAutoUpdated(SyncSavedPaymentMethodFromStripeCommand command) {
        SavedPaymentMethod existing = repository
                .findByStripePaymentMethodId(command.stripePaymentMethodId())
                .orElse(null);
        if (existing == null) {
            // Card not tracked locally — nothing to refresh.
            return Result.success();
        }

        PaymentGatewayPort.SavedPaymentMethodDetails details;
        try {
            details = paymentGateway.retrievePaymentMethodDetails(command.stripePaymentMethodId());
        } catch (PaymentGatewayException e) {
            log.error("[sync-pm] Stripe retrieve failed for {}: {}",
                    command.stripePaymentMethodId(), e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }

        SavedPaymentMethod refreshed = existing.withRefreshedCardDetails(
                details.brand(), details.last4(), details.expMonth(), details.expYear());
        repository.save(refreshed);
        return Result.success();
    }
}
