package io.levelops.ingestion.agent.config;

import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.sinks.GcsStorageDataSink;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class StorageConfig {

    @Bean
    public StorageDataSink storageDataSink(@Value("${INGESTION_BUCKET}") String bucketName,
                                           @Value("${INGESTION_PREFIX:data}") String pathPrefix) {
        log.info("Storage: destination=GCS, bucket={}, prefix={}", bucketName, pathPrefix);
        return new GcsStorageDataSink(bucketName, pathPrefix);
    }

}
