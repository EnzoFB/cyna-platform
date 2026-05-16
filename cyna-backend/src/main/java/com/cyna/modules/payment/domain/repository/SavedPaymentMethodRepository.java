package com.cyna.modules.payment.domain.repository;

import com.cyna.modules.payment.domain.model.SavedPaymentMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedPaymentMethodRepository {
    List<SavedPaymentMethod> findAllByUserId(UUID userId);
    Optional<SavedPaymentMethod> findByIdAndUserId(UUID id, UUID userId);
    /** Used by webhook handlers to find the local row matching a Stripe pm_xxx. */
    Optional<SavedPaymentMethod> findByStripePaymentMethodId(String stripePaymentMethodId);
    SavedPaymentMethod save(SavedPaymentMethod method);
    void deleteById(UUID id);
    /** Used by the {@code payment_method.detached} webhook: idempotent on absent rows. */
    void deleteByStripePaymentMethodId(String stripePaymentMethodId);
    /** RGPD erasure: drop the local card-metadata cache for an anonymized user. */
    void deleteAllByUserId(UUID userId);
    /** Sets is_default=FALSE for every method belonging to this user. */
    void clearDefaultForUser(UUID userId);
    long countByUserId(UUID userId);
}
