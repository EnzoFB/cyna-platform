package com.cyna.shared.domain;

/**
 * Billing frequency of a purchasable subscription line.
 *
 * <p>This is a shared-kernel value object: the same concept is used, with the
 * exact same semantics, across the cart, order, payment and subscription
 * bounded contexts. Keeping a single definition here — alongside {@link Money}
 * — avoids each context reaching into another's domain (which would couple the
 * modules and break their independent deployability).
 */
public enum BillingCycle {
    MONTHLY,
    ANNUAL
}
