package io.levelops.commons.token_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultTestConfiguration {
    private static final Log LOGGER = LogFactory.getLog(DefaultTestConfiguration.class);

    @Bean(name = "custom")
    public ObjectMapper objectMapper() {
        return DefaultObjectMapper.get();
    }
}
