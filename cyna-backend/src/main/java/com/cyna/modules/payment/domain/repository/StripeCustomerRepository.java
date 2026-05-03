package com.cyna.modules.payment.domain.repository;

import java.util.Optional;
import java.util.UUID;

public interface StripeCustomerRepository {
    Optional<String> findStripeCustomerIdByUserId(UUID userId);
    void save(UUID userId, String stripeCustomerId);
}
