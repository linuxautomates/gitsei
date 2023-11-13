package io.levelops.etl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.helper.organization.OrgUsersLockService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.ProductMappingService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.regex.RegexService;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.clients.EventsRESTClient;
import io.levelops.ingestion.services.ControlPlaneJobService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.services.GcsStorageService;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class WorkerConfig {

    @Bean("workerId")
    public String workerId(@Value("${HOSTNAME:worker}") String hostname) {
        return hostname;
    }
    @Bean("etlSchedulerUrl")
    public String etlSchedulerUrl(@Value("${ETL_SCHEDULER_URL:http://etl-service-scheduler}") String hostname) {
        return hostname;
    }

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
    public Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean
    public InventoryService inventoryService(final OkHttpClient client,
                                             final ObjectMapper objectMapper,
                                             @Value("${INVENTORY_SERVICE_URL:http://internal-api-lb}") final String serviceUrl) {
        return new InventoryServiceImpl(serviceUrl, client, objectMapper);
    }

    @Bean
    public ProductMappingService productMappingService(InventoryService inventoryService) {
        return ProductMappingService.builder().inventoryService(inventoryService).build();
    }

    @Bean
    public RegexService regexService() {
        return new RegexService();
    }

    @Bean
    public EventsClient eventsClient(final OkHttpClient client, final ObjectMapper mapper, @Value("${EVENTS_API_URL:http://events-api}") final String apiBaseUrl) {
        return new EventsRESTClient(client, mapper, apiBaseUrl);
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
    public ControlPlaneJobService controlPlaneJobService(ControlPlaneService controlPlaneService) {
        return new ControlPlaneJobService(controlPlaneService, 3);
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(@Value("${REDIS_HOST:redis-service}") String redisHost,
                                                         @Value("${REDIS_PORT:6379}") int redisPort) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new JedisConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public OrgUsersLockService orgUsersLockService(RedisConnectionFactory redisConnectionFactory) {
        return new OrgUsersLockService(redisConnectionFactory);
    }

    @Bean
    public OrgUsersHelper orgUsersHelper(
            OrgUsersDatabaseService orgUsersDatabaseService,
            OrgVersionsDatabaseService orgVersionsDatabaseService,
            OrgUsersLockService orgUsersLockService
    ) {
        return new OrgUsersHelper(orgUsersDatabaseService, orgVersionsDatabaseService, orgUsersLockService);
    }

    @Bean
    public GcsStorageService gcsStorageService(
            @Value("${ETL_PAYLOAD_GCS_BUCKET:etl-payload}") final String etlPayloadBucket) {
        return new GcsStorageService(etlPayloadBucket, "");
    }
}
