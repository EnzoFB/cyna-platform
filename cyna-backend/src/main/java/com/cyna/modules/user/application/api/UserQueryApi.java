package com.cyna.modules.user.application.api;

import java.util.Optional;
import java.util.UUID;

public interface UserQueryApi {
    Optional<UserPaymentView> findUserForPayment(UUID userId);
}
