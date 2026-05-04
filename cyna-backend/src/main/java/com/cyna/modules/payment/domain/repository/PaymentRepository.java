package com.cyna.modules.payment.domain.repository;

import com.cyna.modules.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    void save(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
