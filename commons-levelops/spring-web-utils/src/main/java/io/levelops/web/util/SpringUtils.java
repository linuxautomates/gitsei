package io.levelops.web.util;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.functional.ThrowingSupplier;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Log4j2
public class SpringUtils {

    private static ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
    private static int warnThresholdMs = 100;

    public static void setWarnThresholdMs(int warnThresholdMs) {
        SpringUtils.warnThresholdMs = warnThresholdMs;
    }

    public static void setForkJoinPool(ForkJoinPool forkJoinPool) {
        Validate.notNull(forkJoinPool, "forkJoinPool cannot be null.");
        SpringUtils.forkJoinPool = forkJoinPool;
    }

    public static <T> DeferredResult<ResponseEntity<T>> deferResponse(ThrowingSupplier<ResponseEntity<T>, Exception> bodySupplier) {
        final Optional<String> currentRequestTrace = getCurrentRequestTrace();
        DeferredResult<ResponseEntity<T>> output = new DeferredResult<>();
        forkJoinPool.submit(() -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            try {
                ResponseEntity<T> response = wrapClientException(bodySupplier).get();
                output.setResult(response);
            } catch (Exception e) {
                log.error(e);
                output.setErrorResult(e);
            } finally {
                stopwatch.stop();
                Level level = stopwatch.elapsed(TimeUnit.MILLISECONDS) >= warnThresholdMs ? Level.WARN : Level.DEBUG;
                log.log(level, "Deferred response of {} took {}", currentRequestTrace.orElse("Unknown request"), stopwatch.toString());
            }
        });
        return output;
    }

    // NEEDS TO BE CALLED FROM REQUEST CONTROLLER THREAD
    public static Optional<HttpServletRequest> getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return Optional.empty();
        }
        return Optional.of(((ServletRequestAttributes) requestAttributes).getRequest());
    }

    public static Optional<String> getCurrentRequestTrace() {
        return getCurrentRequest().map(httpReq -> {
            String method = StringUtils.defaultString(httpReq.getMethod());
            String uri = StringUtils.defaultString(httpReq.getRequestURI(), "/UNKNOWN_REQUEST");
            String queryString = httpReq.getQueryString() != null ? "?" + httpReq.getQueryString() : "";
            return  method + " " + uri + queryString;
        });
    }

    private static <T> ThrowingSupplier<T, Exception> wrapClientException(ThrowingSupplier<T, Exception> delegate) {
        return () -> {
            try {
                return delegate.get();
            } catch (Exception e) {
                Throwable rootCause = ExceptionUtils.getRootCause(e);
                if (rootCause instanceof HttpException) {
                    HttpException httpException = (HttpException) rootCause;
                    int code = MoreObjects.firstNonNull(httpException.getCode(), 500);
                    HttpStatus status = MoreObjects.firstNonNull(HttpStatus.resolve(code), HttpStatus.INTERNAL_SERVER_ERROR);
                    String message = parseHttpExceptionMessage(httpException);
                    throw new ResponseStatusException(status, message, httpException);
                }
                throw e;
            }
        };
    }

    private static String parseHttpExceptionMessage(HttpException e) {
        String body = StringUtils.trimToNull(e.getBody());
        if (body != null && body.startsWith("{") && body.endsWith("}")) {
            Map<String, Object> parsed;
            try {
                parsed = DefaultObjectMapper.get().readValue(body, DefaultObjectMapper.get().getTypeFactory()
                        .constructMapType(Map.class, String.class, Object.class));
            } catch (IOException e2) {
                // body doesn't have to be json data, so ignoring errors
                parsed = Collections.emptyMap();
            }
            Object message = parsed.get("message");
            if (message instanceof String) {
                return StringUtils.defaultString((String) message);
            }
        }
        return e.getMessage();
    }
}
