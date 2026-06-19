package com.cyna.modules.payment.domain.repository;

import com.cyna.modules.payment.domain.model.PaymentConsentLog;

import java.util.List;
import java.util.UUID;

public interface PaymentConsentLogRepository {
    PaymentConsentLog save(PaymentConsentLog log);

    /** RGPD Art. 15 transparency — the user's recorded consents, newest first. */
    List<PaymentConsentLog> findAllByUserId(UUID userId);

    /**
     * Removes every consent log for the user. Used on a hard erasure, where
     * there is no legal basis to keep the consent proof (unlike the
     * anonymize-and-keep branch). Idempotent: a no-op when the user has none.
     */
    void deleteAllByUserId(UUID userId);
}
