package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.UserConsentLog;

import java.util.List;
import java.util.UUID;

public interface UserConsentLogRepository {
    UserConsentLog save(UserConsentLog log);

    /** RGPD Art. 15 — the user's recorded consents, newest first. */
    List<UserConsentLog> findAllByUserId(UUID userId);
}
