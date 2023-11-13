package io.levelops.ingestion.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class BaseConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return DefaultObjectMapper.get()
                .registerModule(new Jdk8Module());
    }

}
