package io.levelops.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.notification.clients.SlackBotClientFactory;
import io.levelops.notification.clients.SlackBotInternalClientFactory;
import io.levelops.notification.services.SlackService;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackConfig {
    @Bean
    public SlackBotClientFactory slackClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return SlackBotClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .build();
    }

    @Bean
    public SlackService slackService(SlackBotClientFactory slackBotClientFactory) {
        return new SlackService(slackBotClientFactory);
    }
}
