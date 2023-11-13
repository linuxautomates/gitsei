package io.levelops.rate_limiter;

import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

@Log4j2
public class FrequencyRateLimiter implements RateLimiter {

    private final int requestsPerSeconds;
    private final List<Instant> timestamps = new LinkedList<>();

    public FrequencyRateLimiter(int requestsPerSeconds) {
        this.requestsPerSeconds = requestsPerSeconds;
    }

    @Override
    public boolean acquire() {
        if (requestsPerSeconds <= 0) {
            return true;
        }
        synchronized (this) {
            Instant now = Instant.now();
            expire(now);
            if (timestamps.size() >= requestsPerSeconds) {
                log.debug("Throttled ({} req/s)", requestsPerSeconds);
                return false;
            }
            timestamps.add(now);
            return true;
        }
    }

    private void expire(Instant now) {
        Instant oneSecAgo = now.minusMillis(1);
        timestamps.removeIf(oneSecAgo::isAfter);
    }

}
