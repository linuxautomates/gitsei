package io.levelops.api.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.logging.LoggingWebMvcConfigurer;
import org.springframework.cloud.gcp.logging.TraceIdLoggingWebMvcInterceptor;
import org.springframework.cloud.gcp.logging.extractors.XCloudTraceIdExtractor;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class GcpLoggingConfig extends LoggingWebMvcConfigurer {
    public GcpLoggingConfig() {
        super(new TraceIdLoggingWebMvcInterceptor(new XCloudTraceIdExtractor()), new DefaultGcpProjectIdProvider());
    }
}
