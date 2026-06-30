package com.cyna.modules.payment.domain.repository;

import java.util.Optional;
import java.util.UUID;

public interface StripeCustomerRepository {
    Optional<String> findStripeCustomerIdByUserId(UUID userId);
    /** Reverse lookup used by webhook handlers to attribute a Stripe event to a local user. */
    Optional<UUID> findUserIdByStripeCustomerId(String stripeCustomerId);
    void save(UUID userId, String stripeCustomerId);
}
