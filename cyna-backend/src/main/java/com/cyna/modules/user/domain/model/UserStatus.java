package com.cyna.modules.user.domain.model;

public enum UserStatus {
    ACTIVE,
    /**
     * Account created via self-service registration but not yet email-verified.
     * The user cannot authenticate until they click the unique link mailed at
     * registration (valid 24h), which transitions the account to {@link #ACTIVE}.
     */
    PENDING_VERIFICATION,
    INACTIVE,
    /**
     * Terminal state set by an RGPD Art. 17 erasure request when the account
     * carries a legally-retained transactional footprint (orders/invoices) and
     * therefore cannot be hard-deleted. Direct identifiers are scrubbed; the
     * row survives only as a non-identifying FK anchor for the retained
     * accounting records. An ANONYMIZED account can never authenticate again.
     */
    ANONYMIZED
}
