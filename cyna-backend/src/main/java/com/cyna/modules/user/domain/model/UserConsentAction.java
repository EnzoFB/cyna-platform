package com.cyna.modules.user.domain.model;

/**
 * Consent events tracked in the user area. Stored as a string in
 * {@code user_consent_log.action} with a CHECK constraint mirroring this enum
 * — keep the two in sync when adding a value.
 */
public enum UserConsentAction {
    /**
     * User ticked the mandatory "I accept the Terms of Service and the Privacy
     * Policy" box at registration. Proof of consent under RGPD Art. 7.1 and
     * evidence the Art. 13 information was presented.
     */
    TERMS_AND_PRIVACY
}
