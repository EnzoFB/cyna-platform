package com.cyna.modules.payment.application.api;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort.StripeSubscriptionState;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class PaymentCommandApiImpl implements PaymentCommandApi {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandApiImpl.class);

    private final PaymentGatewayPort gateway;

    PaymentCommandApiImpl(PaymentGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public Result<Void> purgeLocalPaymentDataForUser(UUID userId) {
        // No-op by design: no local card data is kept anymore (Stripe is the
        // sole source of truth). The Stripe Customer (invoices) and the
        // payment_consent_log (RGPD Art. 7.1 proof) are intentionally retained.
        return Result.success();
    }

    @Override
    public Result<StripeSubscriptionState> setStripeSubscriptionCancelAtPeriodEnd(String stripeSubscriptionId,
            boolean cancelAtPeriodEnd) {
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            // Nothing to call at Stripe → nothing authoritative to mirror.
            return Result.success(null);
        }
        try {
            StripeSubscriptionState state =
                    gateway.setSubscriptionCancelAtPeriodEnd(stripeSubscriptionId, cancelAtPeriodEnd);
            return Result.success(state);
        } catch (PaymentGatewayException e) {
            log.error("Stripe cancel_at_period_end={} update failed for {}: {}",
                    cancelAtPeriodEnd, stripeSubscriptionId, e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }
    }
}
