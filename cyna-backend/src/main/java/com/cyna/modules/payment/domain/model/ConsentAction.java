package com.cyna.modules.payment.domain.model;

/**
 * The discrete consent events we track. Stored as a string in
 * {@code payment_consent_log.action} with a CHECK constraint mirroring this
 * enum — keep the two in sync when adding a new value.
 */
public enum ConsentAction {
    /**
     * User ticked "Reuse this card for my next purchases" on the checkout
     * page, accepting that we persist the PaymentMethod id locally for
     * future selection. The card itself stays on Stripe's vaults.
     */
    SAVE_CARD_AT_CHECKOUT
}
