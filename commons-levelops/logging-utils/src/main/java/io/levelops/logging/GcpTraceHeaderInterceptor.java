package io.levelops.logging;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.cloud.gcp.logging.TraceIdLoggingEnhancer;

import java.io.IOException;

/**
 * Forwards the trace id set by GCP Load Balancers if available. This allows us to correlate distributed logs with the
 * same trace id.
 */
public class GcpTraceHeaderInterceptor implements Interceptor {
    public static final String TRACE_HEADER_NAME = "X-Cloud-Trace-Context";

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        String traceId = TraceIdLoggingEnhancer.getCurrentTraceId();
        Request request = chain.request();

        if (!StringUtils.isEmpty(traceId)) {
            return chain.proceed(request.newBuilder()
                    .addHeader(TRACE_HEADER_NAME, traceId)
                    .build());
        } else {
            return chain.proceed(request);
        }
    }
}

