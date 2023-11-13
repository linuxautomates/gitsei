package io.levelops.api.config;

import com.google.common.base.Stopwatch;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
@WebFilter("/*")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PerformanceLogFilter implements Filter {

    @Value("${perf_log.enabled:true}")
    private boolean enabled;

    @Value("${perf_log.warn_threshold_ms:100}")
    private int warnThresholdMs;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Perf log is {}", enabled ? "enabled" : "disabled");
        SpringUtils.setWarnThresholdMs(warnThresholdMs);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        if (!enabled) {
            chain.doFilter(req, resp);
            return;
        }
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            chain.doFilter(req, resp);
        } finally {
            stopwatch.stop();
            Level level = stopwatch.elapsed(TimeUnit.MILLISECONDS) >= warnThresholdMs ? Level.WARN : Level.DEBUG;
            HttpServletRequest httpReq = (HttpServletRequest) req;
            log.log(level, "{} {} took {}",
                    httpReq.getMethod(),
                    httpReq.getRequestURI() + (httpReq.getQueryString() != null ? "?" + httpReq.getQueryString() : ""),
                    stopwatch.toString());
        }
    }

    @Override
    public void destroy() {
    }
}