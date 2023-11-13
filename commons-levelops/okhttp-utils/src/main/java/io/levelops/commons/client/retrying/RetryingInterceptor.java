package io.levelops.commons.client.retrying;

import com.google.common.base.MoreObjects;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.exceptions.RuntimeInterruptedException;
import io.levelops.commons.functional.StreamUtils;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log4j2
public class RetryingInterceptor implements Interceptor {

    private static final Predicate<Response> DEFAULT_PREDICATE = response -> !response.isSuccessful();
    private final int maxAttempts;
    private final long multiplierMs;
    private final long maxWaitMs;
    private final Predicate<Response> retryPredicate;
    private final Function<Response, Long> waitMsExtractor; // if specified, will use the return value as the exact amount of time to wait - if the returned value is null, falls back to the usual strategy
    private final Long waitCutoffMs; // if specified, will stop retrying if the wait time exceeds this value and throw an error. Ignored if waitMsExtractor is not used.

    // @Builder(toBuilder = true) doesn't compile because we have fields that are derived from other fields in the constructor.
    // This is a workaround that allows us to use the toBuilder = true pattern
    public static RetryingInterceptorBuilder defaultRetyerBuilder() {
        List<Integer> excludedErrorCodes = List.of(400, 403, 404, 405);
        return RetryingInterceptor.builder()
                .maxAttempts(10)
                .multiplierMs(400)
                .maxWait(1)
                .maxWaitTimeUnit(TimeUnit.MINUTES)
                .retryPredicate(response -> !response.isSuccessful() && !excludedErrorCodes.contains(response.code()));
    }

    public static RetryingInterceptor buildDefaultRetryer() {
        return defaultRetyerBuilder().build();
    }

    public static RetryingInterceptor buildDefaultRealtimeRetryer() {
        return defaultRetyerBuilder()
                .maxAttempts(2)
                .maxWait(1)
                .maxWaitTimeUnit(TimeUnit.SECONDS)
                .multiplierMs(50)
                .build();
    }

    public static RetryingInterceptor build429Retryer(int maxAttempts, long multiplierMs, long maxWait, TimeUnit maxWaitTimeUnit) {
        return RetryingInterceptor.builder()
                .maxAttempts(maxAttempts)
                .multiplierMs(multiplierMs)
                .maxWait(maxWait)
                .maxWaitTimeUnit(maxWaitTimeUnit)
                .retryPredicate(response -> response.code() == 429)
                .build();
    }

    public static RetryingInterceptor buildHttpCodeRetryer(int maxAttempts, long multiplierMs, long maxWait, TimeUnit maxWaitTimeUnit, Integer... codesToRetry) {
        Set<Integer> codes = StreamUtils.toStream(codesToRetry).filter(Objects::nonNull).collect(Collectors.toSet());
        return RetryingInterceptor.builder()
                .maxAttempts(maxAttempts)
                .multiplierMs(multiplierMs)
                .maxWait(maxWait)
                .maxWaitTimeUnit(maxWaitTimeUnit)
                .retryPredicate(response -> codes.contains(response.code()))
                .build();
    }

    @Builder
    public RetryingInterceptor(int maxAttempts, long multiplierMs, long maxWait, TimeUnit maxWaitTimeUnit,
                               @Nullable Predicate<Response> retryPredicate,
                               Function<Response, Long> waitMsExtractor,
                               Long waitCutoffMs) {
        this.maxAttempts = maxAttempts;
        this.multiplierMs = multiplierMs;
        this.maxWaitMs = maxWaitTimeUnit.toMillis(maxWait);
        this.retryPredicate = ObjectUtils.defaultIfNull(retryPredicate, DEFAULT_PREDICATE);
        this.waitMsExtractor = waitMsExtractor;
        this.waitCutoffMs = waitCutoffMs;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();

        boolean cutoff = false;
        Response response = null;
        for (int attemptNumber = 0; attemptNumber < maxAttempts; attemptNumber++) {
            //ToDo: Change in future to CloseableUtils.closeQuietly
            if (response != null) {
                response.close();
            }
            response = chain.proceed(request);
            if (!retryPredicate.test(response)) {
                return response; // no need to retry, return it
            }

            if (log.isDebugEnabled()) {
                log.debug("Failed request #{}: code={} url={} headers={}", attemptNumber + 1,
                        MoreObjects.firstNonNull(response.code(), 0), request.url().toString(), response.headers());
            }
            log.debug("Retrying request ({}/{})", attemptNumber + 1, maxAttempts);

            if (!wait(attemptNumber, response)) {
                cutoff = true;
                break;
            }
        }

        Throwable cause = null;
        if (response != null) {
            String responseBody = processResponseBody(response);
            cause = new HttpException(MoreObjects.firstNonNull(response.code(), 0), request.method(), request.url().toString(), responseBody, response.headers().toMultimap(), null);
        }

        String message = String.format("Response not successful after retrying %d times", maxAttempts);
        if (cutoff) {
            message += String.format(" (reached wait cutoff of %d ms)", waitCutoffMs);
        }
        throw new IOException(message, cause);
    }

    /**
     * Wait for the appropriate amount of time.
     *
     * @return true if the wait was possible, or false if the cutoff was reached.
     */
    private boolean wait(int attemptNumber, Response response) {
        if (waitMsExtractor != null) {
            Long waitTime = waitMsExtractor.apply(response);
            if (waitTime != null) {
                if (waitCutoffMs != null && waitCutoffMs > 0 && waitTime > waitCutoffMs) {
                    return false;
                }
                sleep(waitTime);
                return true;
            }
        }
        sleepWithExponentialBackoff(attemptNumber);
        return true;
    }

    private String processResponseBody(@Nonnull Response response) {
        ResponseBody body = response.body();
        if (body == null) {
            return null;
        }
        try {
            return body.string();
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                response.close();
            } catch (Exception e) {
                log.warn("Failed to close response body", e);
            }
        }
    }

    protected void sleepWithExponentialBackoff(int attemptNumber) {
        sleep(calculateExponentialBackoff(attemptNumber));
    }

    protected void sleep(long waitMs) {
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeInterruptedException(e);
        }
    }

    protected long calculateExponentialBackoff(int attemptNumber) {
        double exp = Math.pow(2, attemptNumber);
        long result = Math.round(multiplierMs * exp);
        return Math.min(result, maxWaitMs);
    }

}
