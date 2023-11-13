package io.levelops.ingestion.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.levelops.ingestion.engine.CallbackService;
import io.levelops.ingestion.engine.IngestionEngine;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;

@Log4j2
@Configuration
public class IngestionEngineConfig {

    @Value("${ingestion.engine.threads:10}")
    private int nbOfThreads;

    @Bean("onboardingInDays")
    public int onboardingInDays(@Value("${onboarding_in_days:90}") int onboardingInDays) {
        log.info("onboarding_scan_in_days={}", onboardingInDays);
        return onboardingInDays;
    }

    @Bean
    public CallbackService callbackService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return new CallbackService(okHttpClient, objectMapper);
    }

    @Bean
    public IngestionEngine ingestionEngine(ObjectMapper objectMapper, CallbackService callbackService) {
        IngestionEngine ingestionEngine = new IngestionEngine(nbOfThreads, callbackService);
        log.info("Initialized ingestion engine with {} threads.", nbOfThreads);
        return ingestionEngine;
    }

}
