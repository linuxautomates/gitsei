package io.levelops.ingestion.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryServiceImpl;
import okhttp3.OkHttpClient;
import io.levelops.commons.inventory.InventoryService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class InventoryConfig {

    @Value("${INVENTORY_SERVICE_URL:${inventory.url}}")
    private String inventoryServiceUrl;

    @Bean
    public InventoryService inventoryService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        log.info("Initializing InventoryService with url={}", inventoryServiceUrl);
        return InventoryServiceImpl.builder()
                .objectMapper(objectMapper)
                .inventoryServiceUrl(inventoryServiceUrl)
                .client(okHttpClient)
                .build();
    }
}
