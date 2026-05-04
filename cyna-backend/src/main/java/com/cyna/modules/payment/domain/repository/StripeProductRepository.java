package com.cyna.modules.payment.domain.repository;

import java.util.Optional;
import java.util.UUID;

public interface StripeProductRepository {
    Optional<String> findStripeProductIdByCynaProductId(UUID cynaProductId);

    /**
     * Persists the mapping. Idempotent: if a row already exists for the same
     * cyna product, the call is a no-op (handles concurrent ensureProduct calls).
     */
    void save(UUID cynaProductId, String stripeProductId);
}
