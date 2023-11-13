package io.levelops.etl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.services.GcsStorageService;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Log4j2
@Configuration
public class ServiceConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return DefaultObjectMapper.get();
    }

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(1, TimeUnit.MINUTES) // connect timeout
                .writeTimeout(5, TimeUnit.MINUTES) // write timeout
                .readTimeout(5, TimeUnit.MINUTES); // read timeout
        return builder.build();
    }

    @Bean
    public ControlPlaneService controlPlaneService(@Value("${CONTROL_PLANE_URL:http://ingestion-control-plane-lb}") String controlPlaneUrl,
                                                   @Value("${CONTROL_PLANE_OPTIMIZE_GET_ALL_TRIGGER_RESULTS:true}") Boolean optimizeGetAllTriggerResults,
                                                   ObjectMapper objectMapper,
                                                   OkHttpClient client) {
        return ControlPlaneService
                .builder()
                .controlPlaneUrl(controlPlaneUrl)
                .objectMapper(objectMapper)
                .okHttpClient(client)
                .optimizeGetAllTriggerResults(BooleanUtils.isTrue(optimizeGetAllTriggerResults))
                .build();
    }

    @Bean
    public InventoryService inventoryService(final OkHttpClient client,
                                             final ObjectMapper objectMapper,
                                             @Value("${INVENTORY_SERVICE_URL:http://internal-api-lb}") final String serviceUrl) {
        return new InventoryServiceImpl(serviceUrl, client, objectMapper);
    }

    @Bean
    public GcsStorageService gcsStorageService(
            @Value("${ETL_PAYLOAD_GCS_BUCKET:etl-payload}") final String etlPayloadBucket) {
        return new GcsStorageService(etlPayloadBucket, "");
    }
}
