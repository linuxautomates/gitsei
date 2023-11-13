package io.levelops.ingestion.agent.config;

import io.levelops.ingestion.services.IngestionCachingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Optional;

@Configuration
public class CachingConfig {

    @Bean
    public IngestionCachingService ingestionCachingService() {
        // TODO implement proxy caching service similar to LevelopsStorageDataSink
        return new IngestionCachingService() {

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public Optional<String> read(String s, String s1, String s2) {
                return Optional.empty();
            }

            @Override
            public void write(String s, String s1, String s2, String s3) throws IOException {
            }
        };
    }

}
