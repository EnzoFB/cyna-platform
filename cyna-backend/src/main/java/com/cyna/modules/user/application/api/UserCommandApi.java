package com.cyna.modules.user.application.api;

import com.cyna.shared.domain.Result;

import java.util.UUID;

/**
 * Published command API of the user module — the only sanctioned way for other
 * modules to mutate user-owned data. Keeps the user module a leaf in the module
 * graph: orchestrators (e.g. the {@code account} module) call this instead of
 * reaching into {@code user.domain}.
 */
public interface UserCommandApi {

    /**
     * RGPD Art. 17 erasure of the user-owned data. The lawful outcome depends on
     * whether the account carries a legally-retained transactional footprint —
     * a decision the caller makes (it owns the cross-module knowledge of orders):
     * <ul>
     *   <li>{@code hasTransactionalFootprint = true} → anonymize-and-keep: scrub
     *       PII, revoke every session and trusted device, drop addresses (no
     *       standalone retention basis), and raise {@code UserAnonymized}.</li>
     *   <li>{@code hasTransactionalFootprint = false} → hard delete; the FK
     *       {@code ON DELETE CASCADE} chain removes dependent rows.</li>
     * </ul>
     * Returns {@code Result.failure("User not found")} when the account is absent.
     * Does not touch other modules' data (e.g. payment caches) — the caller
     * orchestrates those.
     */
    Result<Void> eraseAccount(UUID userId, boolean hasTransactionalFootprint);
}
