package io.levelops.rate_limiter;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FrequencyRateLimiterTest {

    @Test
    public void test() throws InterruptedException {
        RateLimiter rateLimiter = new FrequencyRateLimiter(0);
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.acquire()).isTrue();
        }

        rateLimiter = new FrequencyRateLimiter(2);
        assertThat(rateLimiter.acquire()).isTrue();
        assertThat(rateLimiter.acquire()).isTrue();
        assertThat(rateLimiter.acquire()).isFalse();

        Thread.sleep(2200);
        assertThat(rateLimiter.acquire()).isTrue();
        assertThat(rateLimiter.acquire()).isTrue();
        assertThat(rateLimiter.acquire()).isFalse();

        rateLimiter.waitForTurn();
        assertThat(rateLimiter.acquire()).isTrue();
        assertThat(rateLimiter.acquire()).isFalse();
    }
}