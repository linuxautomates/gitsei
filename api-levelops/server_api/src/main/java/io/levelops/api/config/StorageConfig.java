package io.levelops.api.config;

import io.levelops.services.GcsStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public GcsStorageService gcsStorageService(@Value("${INGESTION_BUCKET:}") String bucketName,
                                               @Value("${INGESTION_PREFIX:data}") String pathPrefix) {
        return new GcsStorageService(bucketName, pathPrefix);
    }

}
