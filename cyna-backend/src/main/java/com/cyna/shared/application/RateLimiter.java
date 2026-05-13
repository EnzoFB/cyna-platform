package com.cyna.shared.application;

import java.time.Instant;

/**
 * Outbound port for rate limiting. Lives in the application layer so command
 * handlers (e.g. {@code LoginCommandHandler}) can throttle by business-level
 * keys (email, challengeId) without depending on the concrete in-memory or
 * distributed backend.
 */
public interface RateLimiter {

    /**
     * Atomically tries to consume one unit from the bucket identified by
     * {@code key}. Returns a decision describing whether the call was allowed
     * and how many units remain in the current window.
     *
     * <p>{@code now} is injected so tests can use a fake clock without coupling
     * to {@code Clock} on every call site.</p>
     */
    RateLimitDecision consume(String key, int limit, long windowSeconds, Instant now);

    record RateLimitDecision(
            boolean allowed,
            int limit,
            int remaining,
            long retryAfterSeconds
    ) {
        public static RateLimitDecision allowed(int limit, int remaining) {
            return new RateLimitDecision(true, limit, remaining, 0);
        }

        public static RateLimitDecision rejected(int limit, long retryAfterSeconds) {
            return new RateLimitDecision(false, limit, 0, retryAfterSeconds);
        }
    }
}
