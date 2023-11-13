package io.levelops.rate_limiter;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class IntervalRateLimiterTest {

    @Test
    public void test() throws InterruptedException {
        RateLimiter rateLimiter = new IntervalRateLimiter(0);
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.acquire()).isTrue();
        }

        rateLimiter = new IntervalRateLimiter(100);
        assertThat(rateLimiter.acquire()).isTrue();
        assertThat(rateLimiter.acquire()).isFalse();
        assertThat(rateLimiter.acquire()).isFalse();

        Thread.sleep(120);
        assertThat(rateLimiter.acquire()).isTrue();
        assertThat(rateLimiter.acquire()).isFalse();

        Instant t0 = Instant.now();
        rateLimiter.waitForTurn();
        Instant t1 = Instant.now();
        assertThat(rateLimiter.acquire()).isFalse();

        assertThat(Duration.between(t0, t1).abs().getSeconds()).isLessThanOrEqualTo(1);
    }

}