package io.levelops.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.levelops.commons.licensing.service.LicensingService;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LicensingConfig {

    @Bean
    public LicensingService licensingService(ObjectMapper objectMapper,
                                             OkHttpClient okHttpClient,
                                             @Qualifier("licensingServiceUrl") String licensingServiceUrl) {
        return LicensingService.builder()
                .licensingServiceUrl(licensingServiceUrl)
                .objectMapper(objectMapper)
                .client(okHttpClient)
                .build();
    }
}
