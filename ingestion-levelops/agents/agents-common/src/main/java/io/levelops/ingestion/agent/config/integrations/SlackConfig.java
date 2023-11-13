package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.SlackChatInteractiveMessageController;
import io.levelops.ingestion.agent.ingestion.SlackChatMessageController;
import io.levelops.ingestion.agent.ingestion.SlackUserController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.integrations.slack.client.SlackBotClientFactory;
import okhttp3.OkHttpClient;
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
    public SlackChatMessageController slackChatMessageController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            SlackBotClientFactory slackBotClientFactory) {

        return ingestionEngine.add("SlackChatMessageController", SlackChatMessageController.builder()
                .objectMapper(objectMapper)
                .slackBotClientFactory(slackBotClientFactory)
                .build());
    }

    @Bean
    public SlackChatInteractiveMessageController slackChatInteractiveMessageController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            SlackBotClientFactory slackBotClientFactory) {
        return ingestionEngine.add("SlackChatInteractiveMessageController", SlackChatInteractiveMessageController.builder()
                .objectMapper(objectMapper)
                .slackBotClientFactory(slackBotClientFactory)
                .build());
    }

    @Bean
    public SlackUserController slackUserController(
            IngestionEngine ingestionEngine,
            ObjectMapper objectMapper,
            SlackBotClientFactory slackBotClientFactory) {
        return ingestionEngine.add("SlackUserController", SlackUserController.builder()
                .objectMapper(objectMapper)
                .slackBotClientFactory(slackBotClientFactory)
                .build());
    }

}
