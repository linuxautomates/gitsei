package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.PagerDutyIterativeScanController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.pagerduty.client.PagerDutyClientFactory;
import io.levelops.integrations.pagerduty.models.PagerDutyIngestionDataType;
import io.levelops.integrations.pagerduty.sources.PagerDutyIncidentsDataSource;
import io.levelops.integrations.pagerduty.sources.PagerDutyLogEntriesDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PagerDutyConfig {

    @Value("${PAGER_DUTY_OUTPUT_PAGE_SIZE:}")
    private Integer outputPageSize;

    @Bean
    public PagerDutyClientFactory pagerdutyClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return new PagerDutyClientFactory(inventoryService, objectMapper, okHttpClient);
    }

    @Bean
    public PagerDutyLogEntriesDataSource pagerDutyLogEntriesDataSource(IngestionEngine ingestionEngine,
                                                                       PagerDutyClientFactory clientFactory) {
        return ingestionEngine.add(PagerDutyIngestionDataType.LOG_ENTRY.getIngestionPluralDataType(), new PagerDutyLogEntriesDataSource(clientFactory));
    }

    @Bean
    public PagerDutyIncidentsDataSource pagerDutyIncidentsDataSource(IngestionEngine ingestionEngine,
                                                                     PagerDutyClientFactory clientFactory) {
        return ingestionEngine.add(PagerDutyIngestionDataType.INCIDENT.getIngestionPluralDataType(), new PagerDutyIncidentsDataSource(clientFactory));
    }

    @Bean
    public PagerDutyIterativeScanController pagerDutyIterativeScanController(
            IngestionEngine ingestionEngine,
            @Qualifier("onboardingInDays") int onboardingInDays,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            PagerDutyClientFactory pagerDutyClientFactory,
            PagerDutyIncidentsDataSource incidentsDataSource,
            PagerDutyLogEntriesDataSource logEntriesDataSource) {

        return ingestionEngine.add("PagerDutyIterativeScanController", PagerDutyIterativeScanController.builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .clientFactory(pagerDutyClientFactory)
                .onboardingInDays(onboardingInDays)
                .outputPageSize(outputPageSize)
                .incidentsDataSource(incidentsDataSource)
                .logEntriesDataSource(logEntriesDataSource)
                .build());
    }

}
