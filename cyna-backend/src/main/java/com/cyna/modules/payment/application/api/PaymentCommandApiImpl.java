package com.cyna.modules.payment.application.api;

import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.shared.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class PaymentCommandApiImpl implements PaymentCommandApi {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandApiImpl.class);

    private final PaymentGatewayPort gateway;

    PaymentCommandApiImpl(PaymentGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public Result<Void> cancelStripeSubscription(String stripeSubscriptionId) {
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            return Result.success();
        }
        try {
            gateway.cancelSubscription(stripeSubscriptionId);
            return Result.success();
        } catch (PaymentGatewayException e) {
            log.error("Stripe subscription cancellation failed for {}: {}",
                    stripeSubscriptionId, e.getMessage());
            return Result.failure("STRIPE_ERROR: " + e.getMessage());
        }
    }
}
