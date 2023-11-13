package io.levelops.aggregations.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.levelops.aggregations.controllers.AckAggregationsController;
import io.levelops.aggregations.controllers.AggregationsController;
import io.levelops.aggregations.models.messages.AggregationMessage;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.bullseye_converter_clients.BullseyeConverterClient;
import io.levelops.cicd.CiCdRESTClient;
import io.levelops.cicd.services.CiCdService;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.services.dev_productivity.handlers.DevProductivityFeatureHandler;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.ProductMappingService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.licensing.service.LicensingService;
import io.levelops.commons.regex.RegexService;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.clients.EventsRESTClient;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.notification.clients.SlackBotInternalClientFactory;
import io.levelops.plugins.clients.PluginResultsClient;
import io.levelops.plugins.services.JenkinsPluginJobRunCompleteStorageService;
import io.levelops.plugins.services.JenkinsPluginResultPreprocessStorageService;
import io.levelops.plugins.services.PluginResultsStorageService;
import io.levelops.services.EmailService;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.JedisPool;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Configuration
@EnableCaching
public class ServiceConfig {

    @Bean("aggregationControllers")
    @SuppressWarnings("rawtypes")
    public Map<String, AggregationsController> controllerCatalog(@Autowired List<AggregationsController<AggregationMessage>> controllers) {
        log.info("Controllers list: {}", controllers);
        return controllers.stream().collect(Collectors.toMap(AggregationsController::getSubscriptionName, Function.identity()));
    }

    @Bean("ackAggregationControllers")
    @SuppressWarnings("rawtypes")
    public Map<String, AckAggregationsController> ackControllerCatalog(@Autowired List<AckAggregationsController<AggregationMessage>> ackAggControllers) {
        log.info("Controllers list: {}", ackAggControllers);
        return ackAggControllers.stream().collect(Collectors.toMap(AckAggregationsController::getSubscriptionName, Function.identity()));
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
    public ObjectMapper objectMapper() {
        return DefaultObjectMapper.get();
    }

    @Bean
    public Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
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
    public JobDtoParser getJobDtoParser(Storage storage, ObjectMapper objectMapper) {
        return new JobDtoParser(storage, objectMapper);
    }

    @Bean
    public PluginResultsClient pluginResultsClient(OkHttpClient client,
                                                   ObjectMapper objectMapper,
                                                   @Value("${INTERNAL_API_URL:http://internal-api-lb}") String internalApiUri) {
        return new PluginResultsClient(client, objectMapper, internalApiUri);
    }

    @Bean
    public EventsClient eventsClient(final OkHttpClient client, final ObjectMapper mapper, @Value("${EVENTS_API_URL:http://events-api}") final String apiBaseUrl) {
        return new EventsRESTClient(client, mapper, apiBaseUrl);
    }

    @Bean
    public JenkinsPluginResultPreprocessStorageService jenkinsPluginResultPreprocessStorageService(@Value("${PLUGIN_PRE_RESULTS_BUCKET}") String bucketName,
                                                                                                   Storage storage) {
        return new JenkinsPluginResultPreprocessStorageService(storage, bucketName);
    }

    @Bean
    public JenkinsPluginJobRunCompleteStorageService jenkinsPluginJobRunCompleteStorageService(@Value("${PLUGIN_JOB_RUN_COMPLETE_BUCKET}") String bucketName,
                                                                                               Storage storage) {
        return new JenkinsPluginJobRunCompleteStorageService(storage, bucketName);
    }

    @Bean
    public RegexService regexService() {
        return new RegexService();
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(@Value("${REDIS_HOST:redis-service}") String redisHost,
                                                         @Value("${REDIS_PORT:6379}") int redisPort) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new JedisConnectionFactory(config);
    }

    // TODO: Consider using the pooled connections everywhere instead of redisConnectionFactory
    @Bean
    public JedisPool jedisPool(@Value("${REDIS_HOST:redis-service}") String redisHost,
                               @Value("${REDIS_PORT:6379}") int redisPort) {
        return new JedisPool(redisHost, redisPort);
    }

    @Bean
    public CiCdService ciCdService(OkHttpClient client,
                                   ObjectMapper objectMapper,
                                   @Value("${INTERNAL_API_URL:http://internal-api-lb}") String internalApiUri) {
        return new CiCdRESTClient(internalApiUri, client, objectMapper);
    }

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES);
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCacheNames(Set.of("services", "pd_services", "pd_incidents"));
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }

    @Bean
    public PluginResultsStorageService pluginResultsStorageService(@Value("${PLUGIN_RESULTS_BUCKET}") String bucketName,
                                                                   Storage storage,
                                                                   ObjectMapper objectMapper) {
        return new PluginResultsStorageService(bucketName, storage, objectMapper);
    }

    @Bean
    public InventoryService inventoryService(final OkHttpClient client, final ObjectMapper objectMapper,
                                             @Value("${INVENTORY_SERVICE_URL:http://internal-api-lb}") final String serviceUrl) {
        return new InventoryServiceImpl(serviceUrl, client, objectMapper);
    }

    @Bean
    public ProductMappingService productMappingService(InventoryService inventoryService) {
        return ProductMappingService.builder().inventoryService(inventoryService).build();
    }

    @Bean
    public BullseyeConverterClient bullseyeConverterClient(final OkHttpClient client, final ObjectMapper mapper, @Value("${BULLSEYE_CONVERTER_SERVICE_URL}") final String apiBaseUrl) {
        return new BullseyeConverterClient(client, mapper, apiBaseUrl);
    }

    @Bean
    public Map<DevProductivityProfile.FeatureType, DevProductivityFeatureHandler> featureHandlers(List<DevProductivityFeatureHandler> handlers) {
        Map<DevProductivityProfile.FeatureType, DevProductivityFeatureHandler> collectorMap = new HashMap<>();
        for (DevProductivityFeatureHandler handler : handlers) {
            for (DevProductivityProfile.FeatureType featureType : handler.getSupportedFeatureTypes()) {
                collectorMap.put(featureType, handler);
            }
        }
        log.info("featureHandlers count = {}", handlers.size());
        return Collections.unmodifiableMap(collectorMap);
    }

    @Bean
    public SlackBotInternalClientFactory slackBotInternalClientFactory(
            final ObjectMapper objectMapper,
            final OkHttpClient okHttpClient,
            @Value("${INTERNAL_SLACK_TOKEN}") final String token
    ) {
        return new SlackBotInternalClientFactory(
                objectMapper,
                okHttpClient,
                token
        );
    }

    @Bean
    public LicensingService licensingService(
            OkHttpClient client,
            ObjectMapper mapper,
            @Value("${licensingServiceUrl:http://licensing}") String licensingServiceUrl) {
        return new LicensingService(licensingServiceUrl, client, mapper);
    }

    @Bean
    public EmailService emailService(
            @Value("${SENDGRID_API_KEY}") String sendGridApiKey
    ) {
        return new EmailService(sendGridApiKey);
    }

    @Bean("scmCommitsInsertV2integrationIdWhitelist")
    public List<IntegrationWhitelistEntry> scmCommitsInsertV2integrationIdWhitelist (@Value("${SCM_COMMIT_INSERT_V2_INTEGRATION_ID_WHITELIST:}") String scmCommitsInsertV2integrationIdWhitelistString) {
        return IntegrationWhitelistEntry.fromCommaSeparatedString(scmCommitsInsertV2integrationIdWhitelistString);
    }
}