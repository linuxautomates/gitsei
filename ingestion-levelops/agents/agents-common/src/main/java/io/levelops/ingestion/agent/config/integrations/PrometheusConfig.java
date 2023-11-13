package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.PrometheusIngestionController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.prometheus.client.PrometheusClientFactory;
import io.levelops.integrations.prometheus.sources.PrometheusQueryDataSource;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrometheusConfig {

    private static final String PROMETHEUS_QUERY_DATA_SOURCE = "PrometheusQueryDataSource";
    private static final String PROMETHEUS_INGESTION_CONTROLLER = "PrometheusIngestionController";

    @Bean
    public PrometheusClientFactory prometheusClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                           OkHttpClient okHttpClient) {
        return PrometheusClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .build();
    }

    @Bean
    public PrometheusIngestionController prometheusIngestionController(IngestionEngine ingestionEngine,
                                                                       ObjectMapper objectMapper,
                                                                       PrometheusClientFactory clientFactory,
                                                                       StorageDataSink storageDataSink,
                                                                       PrometheusClientFactory prometheusClientFactory) {
        PrometheusQueryDataSource prometheusDataSource = ingestionEngine.add(PROMETHEUS_QUERY_DATA_SOURCE,
                new PrometheusQueryDataSource(prometheusClientFactory));
        return ingestionEngine.add(PROMETHEUS_INGESTION_CONTROLLER, PrometheusIngestionController.builder()
                .clientFactory(clientFactory)
                .objectMapper(objectMapper)
                .prometheusQueryDataSource(prometheusDataSource)
                .storageDataSink(storageDataSink)
                .build());
    }
}
