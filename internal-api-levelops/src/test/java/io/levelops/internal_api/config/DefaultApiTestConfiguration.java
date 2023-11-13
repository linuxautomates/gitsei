package io.levelops.internal_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.AccessKeyService;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.BestPracticesService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdPipelinesAggsService;
import io.levelops.commons.databases.services.ComponentProductMappingService;
import io.levelops.commons.databases.services.ComponentsDatabaseService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.EventTypesDatabaseService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.databases.services.MsTMTDatabaseService;
import io.levelops.commons.databases.services.MsgTemplateService;
import io.levelops.commons.databases.services.OrganizationService;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.commons.databases.services.PluginResultsDatabaseService;
import io.levelops.commons.databases.services.ProductIntegMappingService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.QuestionnaireDBService;
import io.levelops.commons.databases.services.QuestionnaireTemplateDBService;
import io.levelops.commons.databases.services.SamlConfigService;
import io.levelops.commons.databases.services.SectionsService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.databases.services.TicketTemplateDBService;
import io.levelops.commons.databases.services.TokenDataService;
import io.levelops.commons.databases.services.TriggerSchemasDatabaseService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.WorkItemDBService;
import io.levelops.commons.databases.services.WorkItemNotesService;
import io.levelops.commons.databases.services.organization.OrgAccessValidationService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.token_services.AzureDevopsTokenService;
import io.levelops.commons.token_services.BitbucketTokenService;
import io.levelops.commons.token_services.BlackDuckTokenService;
import io.levelops.commons.token_services.CxSastTokenService;
import io.levelops.commons.token_services.GitlabTokenService;
import io.levelops.commons.token_services.MSTeamsTokenService;
import io.levelops.commons.token_services.SalesforceTokenService;
import io.levelops.commons.token_services.SlackTokenService;
import io.levelops.events.clients.EventsClient;
import io.levelops.ingestion.integrations.github.services.GithubIngestionService;
import io.levelops.integrations.github.client.GithubAppTokenService;
import io.levelops.internal_api.services.IntegrationSecretsService;
import io.levelops.internal_api.services.MessagePubService;
import io.levelops.internal_api.services.PluginResultsDiffService;
import io.levelops.internal_api.services.plugins.PluginResultsService;
import io.levelops.internal_api.services.plugins.preprocess.CsvPluginResultPreProcessService;
import io.levelops.internal_api.services.plugins.preprocess.JenkinsPluginResultPreProcessService;
import io.levelops.plugins.services.PluginResultsStorageService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.support.locks.LockRegistry;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class DefaultApiTestConfiguration {

    @Bean
    public DataSource dataSource() {
        var m = Mockito.mock(DataSource.class);
        return m;
    }

    @Bean
    public UserService userService() throws SQLException {
        UserService userService = Mockito.mock(UserService.class);
        when(userService.get(anyString(), anyString())).thenReturn(Optional.of(
            new User("123", "test", "test", "test",
                        RoleType.ADMIN, true, true,
                        false, false,null,  null, Map.of(),null,
                        new User.PasswordReset(null, null), "asd@asd.com",
                        null, null, "test", List.of())));
        when(userService.insert(anyString(), any())).thenReturn("1234");
        when(userService.isTenantSpecific()).thenReturn(true);
        return userService;
    }

    @Bean
    public SlackTokenService slackTokenService() {
        return Mockito.mock(SlackTokenService.class);
    }

    @Bean
    public MSTeamsTokenService msTeamsTokenService() {
        return Mockito.mock(MSTeamsTokenService.class);
    }

    @Bean
    public SalesforceTokenService salesForceTokenService() {
        return Mockito.mock(SalesforceTokenService.class);
    }

    @Bean
    public BitbucketTokenService bitbucketTokenService() {
        return Mockito.mock(BitbucketTokenService.class);
    }

    @Bean
    public GitlabTokenService gitlabTokenService() {
        return Mockito.mock(GitlabTokenService.class);
    }

    @Bean
    public AzureDevopsTokenService azureDevopsTokenService() {
        return Mockito.mock(AzureDevopsTokenService.class);
    }

    @Bean
    public CxSastTokenService cxSastTokenService() {
        return Mockito.mock(CxSastTokenService.class);
    }

    @Bean
    public BlackDuckTokenService blackDuckTokenService() {
        return Mockito.mock(BlackDuckTokenService.class);
    }

    @Bean
    public MsgTemplateService msgTemplateService() {
        var m = Mockito.mock(MsgTemplateService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public Storage storage() {
        return Mockito.mock(Storage.class);
    }

    @Bean(name = "custom")
    @Primary
    public ObjectMapper objectMapper() {
        return DefaultObjectMapper.get();
    }


    @Bean
    public JiraProjectService jiraProjectService() {
        var m = Mockito.mock(JiraProjectService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public AccessKeyService accessKeyService() {
        var m = Mockito.mock(AccessKeyService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public PluginDatabaseService pluginDatabaseService() {
        var m = Mockito.mock(PluginDatabaseService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public WorkItemDBService workItemDBService() {
        var m = Mockito.mock(WorkItemDBService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public WorkItemNotesService workItemNotesService() {
        var m = Mockito.mock(WorkItemNotesService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public DashboardWidgetService dashboardWidgetService() {
        var m = Mockito.mock(DashboardWidgetService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public GitRepositoryService gitRepositoryService() {
        var m = Mockito.mock(GitRepositoryService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }


    @Bean
    public GithubIngestionService githubIngestionService() {
        var m = Mockito.mock(GithubIngestionService.class);
        return m;
    }

    @Bean
    public IntegrationService integrationService() {
        var m = Mockito.mock(IntegrationService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public MessagePubService messagePubService() {
        var m = Mockito.mock(MessagePubService.class);
        return m;
    }

    @Bean
    public ActivityLogService activityLogService() {
        var m = Mockito.mock(ActivityLogService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public TenantService tenantService() {
        var m = Mockito.mock(TenantService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public ProductIntegMappingService productIntegMappingService() {
        var m = Mockito.mock(ProductIntegMappingService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public JiraFieldService jiraFieldService() {
        var m = Mockito.mock(JiraFieldService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public ProductService productService() {
        var m = Mockito.mock(ProductService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();

        System.out.println(m.getClass());
        System.out.println(m.getClass().getSuperclass());

        return m;
    }

    @Bean
    public SamlConfigService samlConfigService() {
        var m = Mockito.mock(SamlConfigService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public TokenDataService tokenDataService() {
        var m = Mockito.mock(TokenDataService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public OrganizationService organizationService() {
        var m = Mockito.mock(OrganizationService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public QuestionnaireDBService questionnaireService() {
        var m = Mockito.mock(QuestionnaireDBService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public QuestionnaireTemplateDBService questionnaireTemplateService() {
        var m = Mockito.mock(QuestionnaireTemplateDBService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public SectionsService questionsService() {
        var m = Mockito.mock(SectionsService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public TagsService tagsService() {
        var m = Mockito.mock(TagsService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public TagItemDBService tagItemService() {
        var m = Mockito.mock(TagItemDBService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public BestPracticesService bestPracticesService() {
        var m = Mockito.mock(BestPracticesService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public DatabaseSchemaService databaseSchemaService() {
        return Mockito.mock(DatabaseSchemaService.class);
    }

    @Bean
    public ComponentProductMappingService componenetProductMappingService() {
        return Mockito.mock(ComponentProductMappingService.class);
    }

    @Bean
    public PluginResultsDatabaseService pluginResultsDatabaseService() {
        var m = mock(PluginResultsDatabaseService.class);
        when(m.isTenantSpecific()).thenCallRealMethod();
        when(m.getReferences()).thenCallRealMethod();
        return m;
    }

    @Bean
    public PluginResultsStorageService pluginResultsStorageService() {
        return Mockito.mock(PluginResultsStorageService.class);
    }

    @Bean
    public PluginResultsDiffService pluginResultsDiffService() {
        return Mockito.mock(PluginResultsDiffService.class);
    }

    @Bean
    public TicketTemplateDBService ticketTemplateDBService() {
        return Mockito.mock(TicketTemplateDBService.class);
    }

    @Bean
    public ComponentsDatabaseService componentTypeDataBaseService() {
        return Mockito.mock(ComponentsDatabaseService.class);
    }

    @Bean
    public EventTypesDatabaseService eventTypesDatabaseService() {
        return Mockito.mock(EventTypesDatabaseService.class);
    }

    @Bean
    public TriggerSchemasDatabaseService triggerSchemasDatabaseService() {
        return Mockito.mock(TriggerSchemasDatabaseService.class);
    }

    @Bean
    public EventsClient eventsClient() {
        return Mockito.mock(EventsClient.class);
    }

    @Bean
    public JenkinsPluginResultPreProcessService jenkinsPluginResultPreProcessService() {
        return Mockito.mock(JenkinsPluginResultPreProcessService.class);
    }

    @Bean
    public CsvPluginResultPreProcessService csvPluginResultPreProcessService() {
        return Mockito.mock(CsvPluginResultPreProcessService.class);
    }

    @Bean
    public CiCdPipelinesAggsService ciCdPipelinesAggsService() {
        return Mockito.mock(CiCdPipelinesAggsService.class);
    }

    @Bean
    public CiCdJobRunStageDatabaseService jobRunStageDatabaseService() {
        return Mockito.mock(CiCdJobRunStageDatabaseService.class);
    }

    @Bean
    public IntegrationSecretsService integrationSecretsService() {
        return Mockito.mock(IntegrationSecretsService.class);
    }

    @Bean
    public MsTMTDatabaseService msTMTDatabaseService() {
        return Mockito.mock(MsTMTDatabaseService.class);
    }

    @Bean
    public PluginResultsService pluginResultsService() {
        return Mockito.mock(PluginResultsService.class);
    }

    @Bean("tokenRefreshLockRegistry")
    public LockRegistry tokenRefreshLockRegistry() throws InterruptedException {
        var lockRegistry = Mockito.mock(LockRegistry.class);
        var lock = Mockito.mock(Lock.class);
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lockRegistry.obtain(anyString())).thenReturn(lock);
        return lockRegistry;
    }

    @Bean
    public OrgVersionsDatabaseService orgVersionsDatabaseService(){
        return Mockito.mock(OrgVersionsDatabaseService.class);
    }

    @Bean
    public OrgUsersDatabaseService orgUsersDatabaseService(){
        return Mockito.mock(OrgUsersDatabaseService.class);
    }

    @Bean
    public OrgUnitHelper orgUnitHelper(){
        return Mockito.mock(OrgUnitHelper.class);
    }

    @Bean
    public OrgAccessValidationService orgAccessValidationService() {
        return Mockito.mock(OrgAccessValidationService.class);
    }

    @Bean
    public GithubAppTokenService GithubAppTokenService() {
        return Mockito.mock(GithubAppTokenService.class);
    }

    @Bean
    TenantConfigService tenantConfigService() {
        return Mockito.mock(TenantConfigService.class);
    }
}
