package io.levelops.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import io.harness.atlassian_connect.AtlassianConnectServiceClient;
import io.harness.authz.acl.client.ACLClientFactory;
import io.levelops.api.converters.AccessKeyRequestConverter;
import io.levelops.api.utils.OrgUserCsvParser;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.RunbookReportDatabaseService;
import io.levelops.commons.databases.services.RunbookReportSectionDatabaseService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.dev_productivity.handlers.DevProductivityFeatureHandler;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.etl.EtlMonitoringClient;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.helper.organization.OrgUsersLockService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.clients.EventsRESTClient;
import io.levelops.files.services.FileStorageService;
import io.levelops.ingestion.integrations.slack.services.SlackIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackUserIngestionService;
import io.levelops.integrations.jira.client.JiraInternalClientFactory;
import io.levelops.logging.GcpTraceHeaderInterceptor;
import io.levelops.logging.TraceEnabledForkJoinPool;
import io.levelops.notification.clients.SlackBotInternalClientFactory;
import io.levelops.notification.services.MSTeamsService;
import io.levelops.notification.services.NotificationService;
import io.levelops.notification.services.SlackService;
import io.levelops.notification.services.TenantManagementNotificationService;
import io.levelops.plugins.clients.PluginsClient;
import io.levelops.runbooks.clients.RunbookClient;
import io.levelops.runbooks.clients.RunbooksRESTClient;
import io.levelops.runbooks.services.RunbookReportService;
import io.levelops.runbooks.services.RunbookReportServiceImpl;
import io.levelops.services.EmailService;
import io.levelops.services.TemplateService;
import io.levelops.tenant_config.clients.TenantConfigClient;
import io.levelops.triggers.clients.TriggersRESTClient;
import io.levelops.web.util.SpringUtils;
import io.levelops.workflow.converters.WorkflowUiDataParser;
import io.propelo.trellis_framework.client.TrellisAPIControllerClient;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Log4j2
@Configuration
@SuppressWarnings("unused")
public class ServerConfig {

    private static final String DEFAULT_INTERNAL_API_SERVICE_URL = "http://internal-api-lb";
    private static final String DEFAULT_TRIGGERS_API_SERVICE_URL = "http://internal-api-lb";
    private static final String DEFAULT_PLUGINS_TRIGGER_API_SERVICE_URL = "http://plugins-trigger-service";
    private static final String DEFAULT_RUNBOOK_API_SERVICE_URL = "http://runbook-api";
    private static final String DEFAULT_GITHUB_WEBHOOK_HANDLING_SERVICE_URL = "http://wh-levelops";
    private static final String DEFAULT_LICENSING_API_SERVICE_URL = "http://licensing";

    @Bean("inventoryServiceUrl")
    public String inventoryServiceUrl() {
        return DEFAULT_INTERNAL_API_SERVICE_URL;
    }

    @Bean("licensingServiceUrl")
    public String licensingServiceUrl() {
        return DEFAULT_LICENSING_API_SERVICE_URL;
    }

    @Bean("githubWebhookHandlingServiceUrl")
    public String githubWebhookHandlingServiceUrl(
            @Value("${GITHUB_WEBHOOK_HANDLING_SERVICE_URL:}") String githubWebhookHandlingServiceUrl) {
        return StringUtils.defaultIfBlank(githubWebhookHandlingServiceUrl, DEFAULT_GITHUB_WEBHOOK_HANDLING_SERVICE_URL);
    }

    @Bean("pluginsTriggerApiUrl")
    public String pluginsTriggerApiUrl() {
        return DEFAULT_PLUGINS_TRIGGER_API_SERVICE_URL;
    }

    @Bean("internalApiUrl")
    public String internalApiUrl() {
        return DEFAULT_INTERNAL_API_SERVICE_URL;
    }

    @Bean("triggersApiUrl")
    public String triggersApiUrl() {
        return DEFAULT_TRIGGERS_API_SERVICE_URL;
    }

    @Bean("runbookApiUrl")
    public String runbookApiUrl(@Value("${RUNBOOK_API_URL:}") String runbookApiUrl) {
        return StringUtils.defaultIfBlank(runbookApiUrl, DEFAULT_RUNBOOK_API_SERVICE_URL);
    }

    @Bean("deferredResponseForkJoinPool")
    public ForkJoinPool deferredResponseForkJoinPool(@Value("${deferred_response_thread_count:32}") int threadCount) {
        var forkJoinPool = new TraceEnabledForkJoinPool(threadCount);
        SpringUtils.setForkJoinPool(forkJoinPool);
        return forkJoinPool;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(1, TimeUnit.MINUTES) // connect timeout
                .writeTimeout(5, TimeUnit.MINUTES) // write timeout
                .readTimeout(5, TimeUnit.MINUTES)
                .addInterceptor(new GcpTraceHeaderInterceptor()); // Forwards the trace id set by GCP load balancers if available
        return builder.build();
    }

    @Bean
    public Storage googleStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean
    public AccessKeyRequestConverter accessKeyRequestConverter(PasswordEncoder passwordEncoder) {
        return new AccessKeyRequestConverter(passwordEncoder);
    }

    @Bean
    public WorkflowUiDataParser workflowUiDataParser(ObjectMapper objectMapper) {
        return new WorkflowUiDataParser(objectMapper);
    }

    @Bean
    public PluginsClient pluginsClient(OkHttpClient client,
                                       ObjectMapper objectMapper,
                                       @Qualifier("pluginsTriggerApiUrl") String url) {
        return new PluginsClient(client, objectMapper, url);
    }

    @Bean
    public TenantConfigClient tenantConfigClient(
            OkHttpClient okHttpClient,
            ObjectMapper objectMapper,
            @Qualifier("internalApiUrl") String internalApiUri) {
        return new TenantConfigClient(okHttpClient, objectMapper, internalApiUri);
    }

    @Bean
    public NotificationService notificationService(EmailService emailService,
                                                   SlackIngestionService slackIngestionService,
                                                   SlackUserIngestionService slackUserIngestionService,
                                                   TemplateService templateService,
                                                   InventoryService inventoryService,
                                                   SlackService slackService,
                                                   MSTeamsService msTeamsService,
                                                   TenantConfigClient tenantConfigClient) {
        return new NotificationService(emailService, slackIngestionService, null, slackUserIngestionService, templateService, inventoryService, slackService, msTeamsService, tenantConfigClient);
    }

    @Bean
    public SlackBotInternalClientFactory slackBotInternalClientFactory(
            ObjectMapper objectMapper, OkHttpClient okHttpClient, @Value("${INTERNAL_SLACK_TOKEN}") String token) {
        return new SlackBotInternalClientFactory(objectMapper, okHttpClient, token);
    }

    @Bean
    public TenantManagementNotificationService tenantManagementNotificationService(EmailService emailService,
                                                                                   SlackBotInternalClientFactory slackBotInternalClientFactory,
                                                                                   JiraInternalClientFactory jiraInternalClientFactory,
                                                                                   TenantConfigService tenantConfigService,
                                                                                   TemplateService templateService,
                                                                                   ObjectMapper objectMapper,
                                                                                   @Value("${OAUTH_BASE_URL}") String baseURL,
                                                                                   @Value("${TENANT_STATUS_SLACK_CHANNEL_ID:C03NCTPS6CW}") String slackChannelId,
                                                                                   @Value("${TENANT_STATUS_EMAIL_DL}") String email,
                                                                                   @Value("${TENANT_STATUS_SLACK_BOT_NAME}") String slackBotName,
                                                                                   @Value("${TENANT_STATUS_JIRA_ASSIGNEE_ACCOUNT_NUMBER}") String jiraAccountNumber,
                                                                                   @Value("${JIRA_ITOPS_PROJECT_ID}") String jiraProjectId
    ) {
        return new TenantManagementNotificationService(slackBotInternalClientFactory, jiraInternalClientFactory, objectMapper, templateService, emailService, tenantConfigService, baseURL, slackChannelId, email, slackBotName, jiraAccountNumber, jiraProjectId);
    }

    @Bean
    public RunbookReportService reportService(@Value("${RUNBOOK_BUCKET_NAME}") String bucketName,
                                              RunbookReportDatabaseService reportDatabaseService,
                                              RunbookReportSectionDatabaseService reportSectionDatabaseService,
                                              ObjectMapper objectMapper,
                                              Storage storage,
                                              @Value("${RUNBOOK_REPORT_PAGE_SIZE:1000}") Integer pageSize) {
        return new RunbookReportServiceImpl(bucketName, reportDatabaseService, reportSectionDatabaseService, objectMapper, storage, pageSize);
    }

    @Bean
    public TriggersRESTClient triggersRESTClient(
            final OkHttpClient client,
            final ObjectMapper objectMapper,
            @Qualifier("triggersApiUrl") final String triggersApiBaseUrl) {
        return new TriggersRESTClient(client, objectMapper, triggersApiBaseUrl);
    }

    @Bean
    public RunbookClient runbookClient(OkHttpClient client, ObjectMapper objectMapper,
                                       @Qualifier("runbookApiUrl") String runbookApiUrl) {
        return new RunbooksRESTClient(client, objectMapper, runbookApiUrl);
    }

    @Bean
    public FileStorageService fileStorageService(@Value("${UPLOADS_BUCKET_NAME:levelops-uploads}") final String bucketName,
                                                 Storage storage) {
        return new FileStorageService(storage, bucketName);
    }

    @Bean
    public SecretGenerator secretGenerator() {
        return new DefaultSecretGenerator();
    }

    @Bean
    public OrgUserCsvParser orgUserCsvParser(final IntegrationService integrationService) {
        return new OrgUserCsvParser(integrationService);
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
    public EventsClient eventsClient(OkHttpClient client,
                                     ObjectMapper mapper,
                                     @Value("${EVENTS_API_URL:http://events-api}") String eventsApiUrl) {
        return new EventsRESTClient(client, mapper, eventsApiUrl);
    }

    @Bean
    public JiraInternalClientFactory jiraInternalClientFactory(
            ObjectMapper objectMapper,
            OkHttpClient okHttpClient,
            @Value("${INTERNAL_JIRA_TOKEN}") String token,
            @Value("${INTERNAL_JIRA_USERNAME}") String userName) {
        return new JiraInternalClientFactory(objectMapper, okHttpClient, 10.0, token, userName);
    }

    @Bean
    public TrellisAPIControllerClient trellisAPIControllerClient(OkHttpClient client,
                                                                 ObjectMapper mapper,
                                                                 @Value("${TRELLIS_API_URL:http://trellis-controller-service-lb}") String trellisApiUrl) {
        return new TrellisAPIControllerClient(client, mapper, trellisApiUrl);
    }


    @Bean
    public EtlMonitoringClient etlMonitoringClient(
            ObjectMapper objectMapper,
            OkHttpClient okHttpClient,
            @Value("${ETL_SCHEDULER_URL:http://etl-service-scheduler}") String etlSchedulerUrl
    ) {
        return new EtlMonitoringClient(okHttpClient, objectMapper, etlSchedulerUrl);
    }

    @Bean
    public AtlassianConnectServiceClient atlassianConnectServiceClient(
            ObjectMapper objectMapper,
            OkHttpClient okHttpClient,
            @Value("${ATLASSIAN_CONNECT_SERVICE_URL:http://atlassian-connect}") String atlassianConnectServiceUrl
    ) {
        return new AtlassianConnectServiceClient(atlassianConnectServiceUrl, okHttpClient, objectMapper);
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
    public Clock getClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ACLClientFactory aclClientFactory(ObjectMapper objectMapper, OkHttpClient okHttpClient,
                                             @Value("${HARNESS_ACS_URL:http://access-control:9006/api/}") String baseUrl){

        return ACLClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .aclUrl(baseUrl)
                .build();
    }
}
