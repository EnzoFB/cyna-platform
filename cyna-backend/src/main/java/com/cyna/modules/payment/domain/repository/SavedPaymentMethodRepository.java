package com.cyna.modules.payment.domain.repository;

import com.cyna.modules.payment.domain.model.SavedPaymentMethod;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedPaymentMethodRepository {
    List<SavedPaymentMethod> findAllByUserId(UUID userId);
    Optional<SavedPaymentMethod> findByIdAndUserId(UUID id, UUID userId);
    SavedPaymentMethod save(SavedPaymentMethod method);
    void deleteById(UUID id);
    /** Sets is_default=FALSE for every method belonging to this user. */
    void clearDefaultForUser(UUID userId);
    long countByUserId(UUID userId);
}
