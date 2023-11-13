package io.levelops.rate_limiter;

import io.levelops.commons.exceptions.RuntimeInterruptedException;
import lombok.extern.log4j.Log4j2;

import java.util.function.Supplier;

@Log4j2
public class ExponentialBackoff {

    private final int baseWaitMs;
    private final int maxWaitMultiplier;
    private final int maxAttempts;

    public ExponentialBackoff() {
        this(10, (1 << 10), 0);
    }

    public ExponentialBackoff(int baseWaitMs, int maxWaitMultiplier, int maxAttempts) {
        this.baseWaitMs = baseWaitMs;
        this.maxWaitMultiplier = maxWaitMultiplier;
        this.maxAttempts = maxAttempts;
    }

    void waitForCondition(Supplier<Boolean> exitCondition) throws RuntimeInterruptedException {
        int multiplier = 1;
        int attempt;
        for (attempt = 0; !Boolean.TRUE.equals(exitCondition.get()); attempt++) {
            if (maxAttempts > 0 && attempt >= maxAttempts) {
                throw new RuntimeException("Exhausted number of attempts");
            }
            int waitMs = multiplier * baseWaitMs;
            log.debug("Waiting for {} ms (attempt={}/{})", waitMs, attempt, maxAttempts);
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            }
            if (multiplier < maxWaitMultiplier) {
                multiplier *= 2;
            }
        }
        log.debug("Acquired after {} attempts (max={})", attempt, maxAttempts);
    }

}
