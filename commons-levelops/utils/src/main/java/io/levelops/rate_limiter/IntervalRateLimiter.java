package io.levelops.rate_limiter;

import lombok.extern.log4j.Log4j2;

import java.time.Instant;

@Log4j2
public class IntervalRateLimiter implements RateLimiter {

    private final int intervalMs;
    private Instant lastRequest = null;

    public IntervalRateLimiter(int intervalMs) {
        this.intervalMs = intervalMs;
    }

    @Override
    public boolean acquire() {
        if (intervalMs <= 0) {
            return true;
        }
        synchronized (this) {
            Instant now = Instant.now();
            Instant oneIntervalAgo = now.minusMillis(intervalMs);
            if (lastRequest == null || lastRequest.isBefore(oneIntervalAgo)) {
                lastRequest = now;
                return true;
            }
            log.debug("Throttled (1 req per {}ms)", intervalMs);
            return false;
        }
    }

}
