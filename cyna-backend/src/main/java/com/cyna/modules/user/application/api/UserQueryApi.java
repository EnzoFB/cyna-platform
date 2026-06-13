package com.cyna.modules.user.application.api;

import java.util.Optional;
import java.util.UUID;

public interface UserQueryApi {
    Optional<UserPaymentView> findUserForPayment(UUID userId);

    Optional<UserNotificationView> findUserForNotification(UUID userId);

    /**
     * RGPD Art. 15/20 — the user-owned slice of the personal-data export
     * (account identity, addresses, terms consents). Empty when the user is
     * absent.
     */
    Optional<UserPersonalDataView> exportPersonalData(UUID userId);
}
