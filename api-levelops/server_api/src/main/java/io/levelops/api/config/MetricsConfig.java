package io.levelops.api.config;

import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;

@Log4j2
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
