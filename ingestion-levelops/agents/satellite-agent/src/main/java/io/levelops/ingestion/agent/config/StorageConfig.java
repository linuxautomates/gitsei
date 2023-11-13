package io.levelops.ingestion.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.sinks.LevelopsStorageDataSink;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class StorageConfig {

    @Bean
    public StorageDataSink storageDataSink(@Qualifier("proxyOkHttpClient") OkHttpClient okHttpClient,
                                           ObjectMapper objectMapper,
                                           SatelliteConfigFileProperties configProperties) {
        log.info("Storage: destination=LevelOps, url={}", configProperties.getSatellite().getUrl());
        return new LevelopsStorageDataSink(okHttpClient, objectMapper, configProperties.getSatellite().getUrl(), configProperties.getSatellite().getApiKey());
    }

}
