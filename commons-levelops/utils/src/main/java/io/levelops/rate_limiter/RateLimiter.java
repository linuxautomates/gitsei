package io.levelops.rate_limiter;

import io.levelops.commons.exceptions.RuntimeInterruptedException;

public interface RateLimiter {

    ExponentialBackoff DEFAULT_WAIT_STRATEGY = new ExponentialBackoff();

    boolean acquire();

    default void waitForTurn() throws RuntimeInterruptedException {
        DEFAULT_WAIT_STRATEGY.waitForCondition(this::acquire);
    }

}
