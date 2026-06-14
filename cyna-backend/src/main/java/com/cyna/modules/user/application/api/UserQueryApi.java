package com.cyna.modules.user.application.api;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserQueryApi {
    Optional<UserPaymentView> findUserForPayment(UUID userId);

    /**
     * Batch identity lookup for cross-module display (e.g. admin order listing).
     * Returns only the users found; missing ids are simply absent.
     */
    List<UserSummaryView> findSummariesByIds(Collection<UUID> userIds);

    Optional<UserNotificationView> findUserForNotification(UUID userId);

    /**
     * RGPD Art. 15/20 — the user-owned slice of the personal-data export
     * (account identity, addresses, terms consents). Empty when the user is
     * absent.
     */
    Optional<UserPersonalDataView> exportPersonalData(UUID userId);

    // ── Reporting (user_schema only) — consumed by the dashboard module ───────

    /** Number of CUSTOMER accounts created in the window. */
    long countCustomersCreatedBetween(Instant fromInclusive, Instant toExclusive);

    /** Number of CUSTOMER accounts created before the given instant. */
    long countCustomersCreatedBefore(Instant beforeExclusive);

    /** Distinct calendar years in which CUSTOMER accounts were created. */
    List<Integer> findCustomerYears();
}
