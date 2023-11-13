package io.levelops.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.notification.clients.msteams.MSTeamsBotClientFactory;
import io.levelops.notification.services.MSTeamsService;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MSTeamsConfig {
    @Bean
    public MSTeamsBotClientFactory msTeamsClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return MSTeamsBotClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .build();
    }

    @Bean
    public MSTeamsService msTeamsService(MSTeamsBotClientFactory msTeamsClientFactory) {
        return new MSTeamsService(msTeamsClientFactory);
    }
}
