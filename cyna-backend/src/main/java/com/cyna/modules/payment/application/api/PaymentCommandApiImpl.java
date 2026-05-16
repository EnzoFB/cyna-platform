package com.cyna.modules.payment.application.api;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.repository.SavedPaymentMethodRepository;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class PaymentCommandApiImpl implements PaymentCommandApi {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandApiImpl.class);

    private final PaymentGatewayPort gateway;
    private final SavedPaymentMethodRepository savedPaymentMethodRepository;

    PaymentCommandApiImpl(PaymentGatewayPort gateway,
                          SavedPaymentMethodRepository savedPaymentMethodRepository) {
        this.gateway = gateway;
        this.savedPaymentMethodRepository = savedPaymentMethodRepository;
    }

    @Override
    public Result<Void> purgeLocalPaymentDataForUser(UUID userId) {
        // Local data minimization only. The Stripe Customer (invoices) and the
        // payment_consent_log (Art. 7.1 proof) are intentionally retained.
        savedPaymentMethodRepository.deleteAllByUserId(userId);
        return Result.success();
    }

    @Override
    public Result<Void> setStripeSubscriptionCancelAtPeriodEnd(String stripeSubscriptionId,
            boolean cancelAtPeriodEnd) {
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            return Result.success();
        }
        try {
            gateway.setSubscriptionCancelAtPeriodEnd(stripeSubscriptionId, cancelAtPeriodEnd);
            return Result.success();
        } catch (PaymentGatewayException e) {
            log.error("Stripe cancel_at_period_end={} update failed for {}: {}",
                    cancelAtPeriodEnd, stripeSubscriptionId, e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }
    }
}
