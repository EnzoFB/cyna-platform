package com.cyna.shared.infrastructure.security;

import com.cyna.shared.application.RateLimiter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class InMemoryRateLimiter implements RateLimiter {

    private static final Duration DEFAULT_BUCKET_TTL = Duration.ofMinutes(10);

    private final Cache<String, WindowCounter> counters = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(DEFAULT_BUCKET_TTL)
            .build();

    @Override
    public RateLimitDecision consume(String key, int limit, long windowSeconds, Instant now) {
        if (limit <= 0) {
            return RateLimitDecision.allowed(0, 0);
        }
        if (windowSeconds <= 0) {
            return RateLimitDecision.allowed(limit, Math.max(0, limit - 1));
        }

        WindowCounter counter = counters.get(key, ignored -> new WindowCounter());
        return counter.consume(limit, windowSeconds, now.getEpochSecond());
    }

    private static final class WindowCounter {
        private long windowStartEpochSecond = -1;
        private int requestCount;

        synchronized RateLimitDecision consume(int limit, long windowSeconds, long nowEpochSecond) {
            long currentWindowStart = (nowEpochSecond / windowSeconds) * windowSeconds;
            if (currentWindowStart != windowStartEpochSecond) {
                windowStartEpochSecond = currentWindowStart;
                requestCount = 0;
            }

            if (requestCount >= limit) {
                long retryAfter = windowSeconds - (nowEpochSecond - windowStartEpochSecond);
                return RateLimitDecision.rejected(limit, Math.max(1, retryAfter));
            }

            requestCount++;
            int remaining = Math.max(0, limit - requestCount);
            return RateLimitDecision.allowed(limit, remaining);
        }
    }
}
