package io.levelops.internal_api.config;

import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import org.springframework.context.annotation.Bean;

public class MetricsConfig {
    @Bean
    public StackdriverMeterRegistry stackdriverMeterRegistry() {
        final StackdriverConfig stackdriverConfig = new StackdriverConfig() {
            @Override
            public String get(String key) {
                return null;
            }
        };
        return StackdriverMeterRegistry.builder(stackdriverConfig).build();
    }
}
