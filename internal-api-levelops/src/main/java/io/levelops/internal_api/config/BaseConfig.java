package io.levelops.internal_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.harness.atlassian_connect.AtlassianConnectServiceClient;
import io.levelops.JsonDiffService;
import io.levelops.aggregations.services.CustomFieldService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.TokenDataService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.helper.organization.OrgUsersLockService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.clients.EventsRESTClient;
import io.levelops.files.services.FileStorageService;
import io.levelops.ingestion.integrations.slack.services.SlackIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackInteractiveIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackUserIngestionService;
import io.levelops.internal_api.controllers.TokensController;
import io.levelops.internal_api.converters.ModifyUserRequestToUserConverter;
import io.levelops.internal_api.models.PluginsSpec;
import io.levelops.internal_api.services.IntegrationSecretsService;
import io.levelops.internal_api.services.InventoryDBService;
import io.levelops.internal_api.services.LocalFilesService;
import io.levelops.notification.clients.SlackBotClientFactory;
import io.levelops.notification.clients.msteams.MSTeamsBotClientFactory;
import io.levelops.notification.services.NotificationService;
import io.levelops.notification.services.MSTeamsService;
import io.levelops.notification.services.SlackService;
import io.levelops.plugins.services.JenkinsPluginJobRunCompleteStorageService;
import io.levelops.plugins.services.JenkinsPluginResultPreprocessStorageService;
import io.levelops.plugins.services.PluginResultsStorageService;
import io.levelops.services.EmailService;
import io.levelops.services.TemplateService;
import io.levelops.tenant_config.clients.TenantConfigClient;
import io.levelops.uploads.services.FilesService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Log4j2
@Configuration
public class BaseConfig {

    @Value("${EVENTS_API_URL:http://events-api}")
    private String eventsApiUrl;

    @Value("${INTERNAL_API_URL:http://internal-api-lb}")
    private String internalApiUrl;

    @Bean("deferredResponseForkJoinPool")
    public ForkJoinPool deferredResponseForkJoinPool(@Value("${deferred_response_thread_count:16}") int threadCount) {
        var forkJoinPool = new ForkJoinPool(threadCount);
        SpringUtils.setForkJoinPool(forkJoinPool);
        return forkJoinPool;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ModifyUserRequestToUserConverter requestToUserConverter(PasswordEncoder passwordEncoder,
                                                                   @Value("${PASSWORD_RESET_TOKEN_EXPIRY_SECONDS:7776000}") Long resetExpiry) {
        return new ModifyUserRequestToUserConverter(passwordEncoder, resetExpiry);
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
    public JsonDiffService jsonDiffService(ObjectMapper objectMapper) {
        return new JsonDiffService(objectMapper);
    }

    @Bean
    public PluginsSpec pluginsSpec() throws IOException {
        // TODO revisit this when custom plugins will be supported (get specs from db?)
        var spec = ResourceUtils.getResourceAsObject("config/plugins_spec.json", PluginsSpec.class);
        log.info("Loaded Plugins spec: {}", Optional.ofNullable(spec).map(PluginsSpec::getPlugins).map(Map::entrySet).orElse(null));
        return spec;
    }

    @Bean
    public EventsClient eventsClient(final OkHttpClient client, final ObjectMapper mapper) {
        return new EventsRESTClient(client, mapper, eventsApiUrl);
    }

    @Bean
    public TemplateService templateService() {
        return new TemplateService();
    }

    @Bean
    public EmailService emailService(@Value("${SENDGRID_API_KEY}") String sendGridApiKey) {
        return new EmailService(sendGridApiKey);
    }

    @Bean
    public InventoryService inventoryService(
            OkHttpClient okHttpClient,
            IntegrationService integrationService,
            @Value("${READ_TOKENS_FROM_SECRETS_MANAGER_SERVICE:false}") boolean readTokensFromSecretsManagerService,
            @Value("#{'${USE_SECRETS_MANAGER_SERVICE_FOR_INTEGRATIONS:}'.split(',')}") List<String> useSecretsManagerServiceForIntegrations,
            IntegrationSecretsService integrationSecretsService,
            TokenDataService tokenService) {
        return InventoryDBService.builder()
                .integrationService(integrationService)
                .readTokensFromSecretsManagerService(readTokensFromSecretsManagerService)
                .useSecretsManagerServiceForIntegrations(TokensController.parseListOfTenantAtIntegrationIds(useSecretsManagerServiceForIntegrations))
                .integrationSecretsService(integrationSecretsService)
                .tokenDataService(tokenService)
                .build();
    }

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

    @Bean
    public MSTeamsBotClientFactory msTeamsBotClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return MSTeamsBotClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .build();
    }

    @Bean
    public MSTeamsService msTeamsService(MSTeamsBotClientFactory msTeamsBotClientFactory) {
        return new MSTeamsService(msTeamsBotClientFactory);
    }

    @Bean
    public TenantConfigClient tenantConfigClient(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        return new TenantConfigClient(okHttpClient, objectMapper, internalApiUrl);
    }

    @Bean
    public NotificationService notificationService(EmailService emailService,
                                                   SlackIngestionService slackIngestionService,
                                                   SlackInteractiveIngestionService slackInteractiveIngestionService,
                                                   SlackUserIngestionService slackUserIngestionService,
                                                   TemplateService templateService,
                                                   InventoryService inventoryService,
                                                   SlackService slackService,
                                                   MSTeamsService msTeamsService,
                                                   TenantConfigClient tenantConfigClient) {
        return new NotificationService(emailService, slackIngestionService, slackInteractiveIngestionService, slackUserIngestionService, templateService, inventoryService, slackService, msTeamsService, tenantConfigClient);
    }

    @Bean
    public FilesService filesService(@Value("${UPLOADS_BUCKET_NAME}") final String bucketName, final Storage storage) {
        return new LocalFilesService(bucketName, storage);
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
    public PluginResultsStorageService pluginResultsStorageService(@Value("${PLUGIN_RESULTS_BUCKET}") String bucketName,
                                                                   Storage storage,
                                                                   ObjectMapper objectMapper) {
        return new PluginResultsStorageService(bucketName, storage, objectMapper);
    }

    @Bean
    public FileStorageService fileStorageService(@Value("${UPLOADS_BUCKET_NAME:levelops-uploads}") final String bucketName,
                                                 Storage storage) {
        return new FileStorageService(storage, bucketName);
    }

    @Bean
    public SecretsManagerServiceClient secretsManagerServiceClient(OkHttpClient okHttpClient,
                                                                   ObjectMapper objectMapper,
                                                                   @Value("${SECRETS_MANAGER_SERVICE_URL:http://secrets-manager-service}") String secretsManagerServiceUrl) {
        return new SecretsManagerServiceClient(secretsManagerServiceUrl, okHttpClient, objectMapper);
    }

    @Bean
    AtlassianConnectServiceClient atlassianConnectServiceClient(OkHttpClient okHttpClient,
                                                                ObjectMapper objectMapper,
                                                                @Value("${ATLASSIAN_CONNECT_SERVICE_URL:http://atlassian-connect}") String atlassianConnectServiceUrl) {
        return new AtlassianConnectServiceClient(atlassianConnectServiceUrl, okHttpClient, objectMapper);
    }

    @Bean
    public CustomFieldService customFieldService(IntegrationService integrationService) {
        return new CustomFieldService(integrationService);
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
}
