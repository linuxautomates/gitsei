package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.ZendeskController;
import io.levelops.ingestion.agent.ingestion.ZendeskIngestionController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.zendesk.client.ZendeskClientFactory;
import io.levelops.integrations.zendesk.services.ZendeskTicketEnrichmentService;
import io.levelops.integrations.zendesk.sources.ZendeskFieldDataSource;
import io.levelops.integrations.zendesk.sources.ZendeskMetricEventDataSource;
import io.levelops.integrations.zendesk.sources.ZendeskTicketDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZendeskConfig {

    private static final String ZENDESK_TICKET_DATA_SOURCE = "ZendeskTicketDataSource";
    private static final String ZENDESK_CONTROLLER = "ZendeskController";
    private static final String ZENDESK_TICKET_METRIC_EVENT_DATA_SOURCE = "ZendeskTicketMetricEventDataSource";
    private static final String ZENDESK_INGESTION_CONTROLLER = "ZendeskIngestionController";
    private static final String ZENDESK_FIELD_DATA_SOURCE = "ZendeskFieldDataSource";

    @Bean
    public ZendeskClientFactory zendeskClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                     OkHttpClient okHttpClient,
                                                     @Value("${zendesk_response_page_size:200}") int pageSize) {
        return ZendeskClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .pageSize(pageSize)
                .build();
    }

    @Bean
    public ZendeskTicketEnrichmentService zendeskTicketService(@Value("${zendesk_fork_threshold:32}") int forkThreshold,
                                                               @Value("${zendesk_thread_count:8}") int threadCount) {
        return new ZendeskTicketEnrichmentService(threadCount, forkThreshold);
    }

    @Bean
    public ZendeskTicketDataSource zendeskTicketDataSource(IngestionEngine ingestionEngine,
                                                           ZendeskClientFactory zendeskClientFactory,
                                                           ZendeskTicketEnrichmentService enrichmentService) {
        return ingestionEngine.add(ZENDESK_TICKET_DATA_SOURCE, new ZendeskTicketDataSource(zendeskClientFactory,
                enrichmentService));
    }

    @Bean
    public ZendeskMetricEventDataSource zendeskTicketMetricEventDataSource(IngestionEngine ingestionEngine,
                                                                           ZendeskClientFactory clientFactory) {
        return ingestionEngine.add(ZENDESK_TICKET_METRIC_EVENT_DATA_SOURCE, new ZendeskMetricEventDataSource(clientFactory));
    }

    @Bean
    public ZendeskFieldDataSource zendeskFieldDataSource(IngestionEngine ingestionEngine,
                                                                     ZendeskClientFactory clientFactory) {
        return ingestionEngine.add(ZENDESK_FIELD_DATA_SOURCE, new ZendeskFieldDataSource(clientFactory));
    }

    @Bean
    public ZendeskController zendeskController(IngestionEngine ingestionEngine, ZendeskTicketDataSource dataSource,
                                               ZendeskMetricEventDataSource metricEventDataSource,
                                               ZendeskFieldDataSource fieldDataSource,
                                               ObjectMapper objectMapper, StorageDataSink storageDataSink,
                                               @Qualifier("onboardingInDays") int onboardingInDays) {
        return ingestionEngine.add(ZENDESK_CONTROLLER, ZendeskController.builder()
                .ticketDataSource(dataSource)
                .metricEventDataSource(metricEventDataSource)
                .fieldDataSource(fieldDataSource)
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .onboardingScanInDays(onboardingInDays)
                .build());
    }


    @Bean
    public ZendeskIngestionController zendeskIngestionController(IngestionEngine ingestionEngine,
                                                                 ObjectMapper objectMapper,
                                                                 ZendeskClientFactory clientFactory) {
        return ingestionEngine.add(ZENDESK_INGESTION_CONTROLLER, ZendeskIngestionController.builder()
                .clientFactory(clientFactory)
                .objectMapper(objectMapper)
                .build());
    }

}
