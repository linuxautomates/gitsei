package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.agent.ingestion.CustomRestCallController;
import io.levelops.ingestion.engine.IngestionEngine;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomIntegrationConfig {

    @Bean
    public CustomRestCallController customRestCallController(IngestionEngine ingestionEngine, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return ingestionEngine.add("CustomRestCallController", new CustomRestCallController(objectMapper, okHttpClient));
    }

}
