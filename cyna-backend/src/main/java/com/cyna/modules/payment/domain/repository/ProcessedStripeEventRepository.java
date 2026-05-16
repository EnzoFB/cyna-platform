package com.cyna.modules.payment.domain.repository;

/**
 * Deduplication ledger for Stripe's at-least-once webhook delivery.
 *
 * <p>Strategy: <em>record-after-success</em>. The handler pre-checks with
 * {@link #isAlreadyProcessed} and only calls {@link #markProcessed} once the
 * event was handled successfully. This is strictly safer than claim-before:
 *
 * <ul>
 *   <li>duplicate delivered after a success → pre-check finds it → skipped;</li>
 *   <li>handler fails → event is NOT recorded → Stripe retries → reprocessed
 *       (no event ever lost);</li>
 *   <li>two concurrent first deliveries → both pass the pre-check and process,
 *       but the underlying domain operations are idempotent (order-pay state
 *       checks, Stripe idempotency keys, idempotent cancellation), so the
 *       outcome is safe; both then race on {@link #markProcessed}, which is a
 *       no-op on conflict.</li>
 * </ul>
 */
public interface ProcessedStripeEventRepository {

    /** True if this event id was already processed successfully (duplicate). */
    boolean isAlreadyProcessed(String eventId);

    /**
     * Records the event id as successfully processed. Idempotent: a concurrent
     * duplicate that also reaches this point is silently ignored (the database
     * arbitrates via {@code ON CONFLICT DO NOTHING}).
     */
    void markProcessed(String eventId, String eventType);
}
