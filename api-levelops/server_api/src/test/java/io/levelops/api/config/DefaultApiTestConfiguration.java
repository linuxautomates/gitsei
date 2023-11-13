package io.levelops.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.harness.atlassian_connect.AtlassianConnectServiceClient;
import io.harness.atlassian_connect.exceptions.AtlassianConnectServiceClientException;
import io.harness.authz.acl.client.ACLClientFactory;
import io.levelops.api.controllers.ConfigTableHelper;
import io.levelops.api.exceptions.PreflightCheckFailedException;
import io.levelops.api.services.DoraService;
import io.levelops.api.services.GcpLoggingService;
import io.levelops.api.services.IngestedAtCachingService;
import io.levelops.api.services.IntegrationManagementService;
import io.levelops.api.services.JiraIssueApiService;
import io.levelops.api.services.JiraSprintMetricsService;
import io.levelops.api.services.JiraSprintMetricsServiceLegacy;
import io.levelops.api.services.TagItemService;
import io.levelops.api.services.dev_productivity.DevProductivityOpsService;
import io.levelops.auth.auth.authobject.ExtendedUser;
import io.levelops.auth.auth.config.Auth;
import io.levelops.auth.auth.config.JwtAuthenticationEntryPoint;
import io.levelops.auth.auth.filter.AuthRequestFilter;
import io.levelops.auth.auth.filter.HarnessAuthRequestFilter;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.services.SystemNotificationService;
import io.levelops.auth.utils.ControllerMethodFinder;
import io.levelops.auth.utils.JwtTokenUtil;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.databases.services.jira.VelocityStageTimesReportPrecalculateWidgetService;
import io.levelops.commons.databases.services.jira.conditions.JiraFieldConditionsBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.generic.clients.GenericRequestsClient;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.service.dora.ADODoraService;
import io.levelops.commons.service.dora.CiCdDoraService;
import io.levelops.commons.service.dora.JiraDoraService;
import io.levelops.commons.service.dora.LegacyLeadTimeCalculationService;
import io.levelops.commons.service.dora.ScmDoraService;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsService;
import io.levelops.commons.services.velocity_productivity.services.VelocityConfigsService;
import io.levelops.commons.token_services.*;
import io.levelops.faceted_search.services.scm_service.EsScmPRsService;
import io.levelops.faceted_search.services.workitems.EsJiraIssueQueryService;
import io.levelops.faceted_search.services.workitems.EsWorkItemsQueryService;
import io.levelops.ingestion.integrations.slack.services.SlackIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackUserIngestionService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.ingestion.services.GenericIngestionService;
import io.levelops.integrations.github.client.GithubAppTokenService;
import io.levelops.models.PreflightCheckResults;
import io.levelops.notification.services.MSTeamsService;
import io.levelops.notification.services.NotificationService;
import io.levelops.notification.services.SlackService;
import io.levelops.notification.services.TenantManagementNotificationService;
import io.levelops.plugins.clients.PluginResultsClient;
import io.levelops.preflightchecks.PreflightCheck;
import io.levelops.runbooks.services.RunbookReportService;
import io.levelops.services.EmailService;
import io.levelops.services.PreflightCheckService;
import io.levelops.services.TemplateService;
import io.levelops.triggers.clients.TriggersRESTClient;
import io.levelops.web.exceptions.BadRequestException;
import okhttp3.OkHttpClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;


import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Configuration
public class DefaultApiTestConfiguration {

    @Bean
    public AuthDetailsService jwtUserDetailsService() throws SQLException, ExecutionException, IllegalAccessException {
        AuthDetailsService dataSource = Mockito.mock(AuthDetailsService.class);
        when(dataSource.loadUserByUsernameAndOrg(eq("admin1"), eq("test")))
                .thenAnswer(ans -> new ExtendedUser("admin1",
                        "$2a$10$zaL07.61u3VpZcq69V46Zu5ZAIrbtEXmUKAQwqXjL8UWDCpUzRCFy", true,
                        true, RoleType.ADMIN, "Admin1", "LastName1", Collections.emptyList(), false, false, false, null));
        when(dataSource.loadUserByUsernameAndOrg(eq("harsh"), eq("test")))
                .thenAnswer(ans -> new ExtendedUser("harsh",
                        "$2a$10$zaL07.61u3VpZcq69V46Zu5ZAIrbtEXmUKAQwqXjL8UWDCpUzRCFy", true,
                        true, RoleType.ADMIN, "harsh", "harsh", Collections.emptyList(), false, false, false, null));
        when(dataSource.loadUserByUsernameAndOrg(eq("harsh2"), eq("test")))
                .thenAnswer(ans -> new ExtendedUser("harsh2",
                        "$2a$10$zaL07.61u3VpZcq69V46Zu5ZAIrbtEXmUKAQwqXjL8UWDCpUzRCFy", false,
                        false, RoleType.ADMIN, "harsh", "harsh", Collections.emptyList(), false, false, false, null));
        when(dataSource.loadUserByUsernameAndOrg(eq("limiteduser"), eq("test")))
                .thenAnswer(ans -> new ExtendedUser("harsh2",
                        "$2a$10$zaL07.61u3VpZcq69V46Zu5ZAIrbtEXmUKAQwqXjL8UWDCpUzRCFy", false,
                        false, RoleType.LIMITED_USER, "harsh", "harsh", Collections.emptyList(), false, false, false, null));
        when(dataSource.validateKeyAndGetRole(eq("test"), eq("harsh"), any()))
                .thenAnswer(ans -> RoleType.ADMIN);
        return dataSource;
    }

    @Bean
    public SlackTokenService.SlackSecrets slackSecrets() {
        return Mockito.mock(SlackTokenService.SlackSecrets.class);
    }

    @Bean
    public SlackTokenService slackTokenService() {
        return Mockito.mock(SlackTokenService.class);
    }

    @Bean
    public MSTeamsTokenService.MSTeamsSecrets msTeamsSecrets() {
        return Mockito.mock(MSTeamsTokenService.MSTeamsSecrets.class);
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
    public SlackTenantLookupDatabaseService slackTenantLookupDatabaseService() {
        return Mockito.mock(SlackTenantLookupDatabaseService.class);
    }

    @Bean
    public ActivityLogService activityLogService() {
        return Mockito.mock(ActivityLogService.class);
    }

    @Bean
    public TenantConfigService tenantConfigService() {
        return Mockito.mock(TenantConfigService.class);
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        JwtTokenUtil tokenUtil = new JwtTokenUtil("asdasdasdasdasdasdasdasdasdasdasdasdasdasd");
        JwtTokenUtil spy = spy(tokenUtil);
        doReturn(1000L).when(spy).getJwtTokenValiditySeconds();
        return spy;
    }

    @Bean
    public AuthRequestFilter jwtRequestFilter(JwtTokenUtil jwtTokenUtil,
                                              ObjectMapper objectMapper,
                                              AuthDetailsService detailsService,
                                              RedisConnectionFactory redisFactory, Auth auth) {
        return new AuthRequestFilter(jwtTokenUtil, detailsService, objectMapper, redisFactory, false, auth);
    }

    @Bean
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping(){
        return new RequestMappingHandlerMapping();
    }
    @Bean
    public Auth getAuth(){
        return new Auth(true);
    }
    @Bean
    public HarnessAuthRequestFilter harnessAuthzRquestFilter(ACLClientFactory aclClientFactory, ControllerMethodFinder controllerMethodFinder, ObjectMapper objectMapper, JwtTokenUtil jwtTokenUtil,
                                                             Auth auth, AuthDetailsService authDetailsService) {
        return new HarnessAuthRequestFilter(aclClientFactory, controllerMethodFinder, objectMapper, jwtTokenUtil, "identityServiceSecret", auth, Set.of(), authDetailsService);
    }

    @Bean
    public ACLClientFactory aclClientFactory(ObjectMapper objectMapper,
                                             @Value("${HARNESS_ACS_URL:http://access-control:9006/api/}") String baseUrl){

        return ACLClientFactory.builder()
                .okHttpClient(new OkHttpClient())
                .objectMapper(objectMapper)
                .aclUrl(baseUrl)
                .build();
    }


    @Bean
    public ControllerMethodFinder controllerMethodFinder(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        return new ControllerMethodFinder(requestMappingHandlerMapping);
    }

    @Bean
    public SystemNotificationService systemNotificationService() {
        return Mockito.mock(SystemNotificationService.class);
    }

    @Bean
    public ControlPlaneService controlPlaneService() {
        return Mockito.mock(ControlPlaneService.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DataSource dataSource() {
        return Mockito.mock(DataSource.class);
    }

    @Bean
    public CiCdInstancesDatabaseService getCiCdInstancesDatabaseService() {
        return Mockito.mock(CiCdInstancesDatabaseService.class);
    }

    @Bean
    public UserService userService() throws SQLException {
        UserService userService = Mockito.mock(UserService.class);
        when(userService.get(anyString(), anyString())).thenReturn(
                Optional.of(User.builder()
                        .id("123")
                        .firstName("test")
                        .lastName("test")
                        .bcryptPassword("test")
                        .userType(RoleType.ADMIN)
                        .passwordAuthEnabled(true)
                        .samlAuthEnabled(true)
                        .passwordResetDetails(new User.PasswordReset())
                        .email("asd@asd.com")
                        .build()));
        when(userService.insert(anyString(), any())).thenReturn("1234");
        return userService;
    }

    @Bean(name = "custom")
    @Primary
    public ObjectMapper objectMapper() {
        return DefaultObjectMapper.get();
    }

    @Bean
    public DashboardWidgetService dashboardWidgetService() {
        return Mockito.mock(DashboardWidgetService.class);
    }

    @Bean
    public JiraProjectService jiraProjectService() {
        return Mockito.mock(JiraProjectService.class);
    }

    @Bean
    public GitRepositoryService githubRepositoryService() {
        return Mockito.mock(GitRepositoryService.class);
    }

    @Bean
    public GenericIngestionService genericIngestionService() {
        GenericIngestionService genericIngestionService = Mockito.mock(GenericIngestionService.class);
        return genericIngestionService;
    }

    @Bean
    public SamlConfigService samlConfigService() {
        return Mockito.mock(SamlConfigService.class);
    }

    @Bean
    public InventoryService inventoryService() {
        return Mockito.mock(InventoryService.class);
    }

    @Bean
    public OrganizationService organizationService() {
        OrganizationService organizationService = Mockito.mock(OrganizationService.class);
        return organizationService;
    }

//    @Bean
//    public TeamService teamService() {
//        Team team = Team.builder().name("team1").id("1").organizationId("1").build();
//        TeamService teamService = Mockito.mock(TeamService.class);
//        when(teamService.get("test", "1")).thenReturn(Optional.of(team));
//        return teamService;
//    }

    @Bean
    public NotificationService notificationService() {
        return Mockito.mock(NotificationService.class);
    }

    @Bean
    public GithubTokenService githubTokenService() {
        return Mockito.mock(GithubTokenService.class);
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
    public ServicenowTokenService servicenowTokenService() {
        return Mockito.mock(ServicenowTokenService.class);
    }

    @Bean
    public GithubAppTokenService GithubAppTokenService() {
        return Mockito.mock(GithubAppTokenService.class);
    }


    @Bean
    public IntegrationManagementService createIntegrationService() throws InventoryException, PreflightCheckFailedException, BadRequestException, AtlassianConnectServiceClientException {
        IntegrationManagementService mocked = Mockito.mock(IntegrationManagementService.class);
        when(mocked.createIntegrationFromRequest(any(), any())).thenReturn(Integration.builder().id("1").build());
        return mocked;
    }

    @Bean
    public SectionsService questionsService() {
        return Mockito.mock(SectionsService.class);
    }

    @Bean
    public Storage storage() {
        return Mockito.mock(Storage.class);
    }

    @Bean
    public TagItemDBService tagItemDBService() {
        return Mockito.mock(TagItemDBService.class);
    }

    @Bean
    public TagItemService tagItemService() {
        return Mockito.mock(TagItemService.class);
    }

    @Bean
    public AccessKeyService accessKeyService() {
        return Mockito.mock(AccessKeyService.class);
    }

    @Bean
    public TagsService tagsService() {
        return Mockito.mock(TagsService.class);
    }

    @Bean
    public EmailService emailService() {
        return Mockito.mock(EmailService.class);
    }

    @Bean
    public PreflightCheckService preflightCheckService() {
        PreflightCheck jiraPreflightCheck = Mockito.mock(PreflightCheck.class);
        when(jiraPreflightCheck.getIntegrationType()).thenReturn("jira");
        when(jiraPreflightCheck.check(anyString(), any(Integration.class), any(Token.class))).thenReturn(PreflightCheckResults.builder()
                .success(true)
                .build());
        when(jiraPreflightCheck.check(eq("failPreflightCheck"), any(Integration.class), any(Token.class))).thenReturn(PreflightCheckResults.builder()
                .success(false)
                .build());
        return new PreflightCheckService(List.of(
                jiraPreflightCheck
        ));
    }

    @Bean
    public PluginResultsDatabaseService pluginResultsDatabaseService() {
        return Mockito.mock(PluginResultsDatabaseService.class);
    }

    @Bean
    public PluginDatabaseService pluginDatabaseService() {
        return Mockito.mock(PluginDatabaseService.class);
    }

    @Bean
    public PluginResultsClient pluginResultsClient() {
        return Mockito.mock(PluginResultsClient.class);
    }

    @Bean
    public StateDBService stateDBService() {
        return Mockito.mock(StateDBService.class);
    }

    @Bean
    public QuestionnaireTemplateDBService questionnaireTemplateDBService() {
        return Mockito.mock(QuestionnaireTemplateDBService.class);
    }

    @Bean
    public SlackIngestionService slackIngestionService() {
        return Mockito.mock(SlackIngestionService.class);
    }

    @Bean
    public SlackUserIngestionService slackUserIngestionService() {
        return Mockito.mock(SlackUserIngestionService.class);
    }

    @Bean
    public SlackService slackService() {
        return Mockito.mock(SlackService.class);
    }

    @Bean
    public MSTeamsService msTeamsService() {
        return Mockito.mock(MSTeamsService.class);
    }

    @Bean
    public TemplateService templateService() {
        return Mockito.mock(TemplateService.class);
    }

    @Bean
    public TriggersRESTClient getTriggersRESTClient() {
        return Mockito.mock(TriggersRESTClient.class);
    }

    @Bean
    public RunbookReportDatabaseService runbookReportDatabaseService() {
        return Mockito.mock(RunbookReportDatabaseService.class);
    }

    @Bean

    public RunbookReportSectionDatabaseService runbookReportSectionDatabaseService() {
        return Mockito.mock(RunbookReportSectionDatabaseService.class);
    }

    @Bean
    public RunbookReportService reportService() {
        return Mockito.mock(RunbookReportService.class);
    }

    @Bean
    public ProductsDatabaseService productsDatabaseService() {
        return Mockito.mock(ProductsDatabaseService.class);
    }

    @Bean
    public ProductService productsService() {
        return Mockito.mock(ProductService.class);
    }

    @Bean
    public ProductIntegMappingService productIntegMappingService() {
        return Mockito.mock(ProductIntegMappingService.class);
    }

    @Bean
    public IntegrationService integrationService() {
        return Mockito.mock(IntegrationService.class);
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    public OrgVersionsDatabaseService orgVersionsDatabaseService() {
        return Mockito.mock(OrgVersionsDatabaseService.class);
    }

    @Bean
    public OrgUsersDatabaseService orgUsersDatabaseService() {
        return Mockito.mock(OrgUsersDatabaseService.class);
    }

    @Bean
    public OrgUsersHelper orgUsersHelper() {
        return Mockito.mock(OrgUsersHelper.class);
    }

    @Bean
    public TenantManagementNotificationService tenantManagementNotificationService() {
        return Mockito.mock(TenantManagementNotificationService.class);
    }

    @Bean
    public OrgUnitHelper orgUnitHelper() {
        return Mockito.mock(OrgUnitHelper.class);
    }

    @Bean
    public CiCdDoraService ciCdAggsService() {
        return Mockito.mock(CiCdDoraService.class);
    }

    @Bean
    JiraFilterParser jiraFilterParser() {
        return Mockito.mock(JiraFilterParser.class);

    }

    @Bean
    JiraDoraService jiraDoraService() {
        return Mockito.mock(JiraDoraService.class);

    }

    @Bean
    ScmDoraService scmDoraService() {
        return Mockito.mock(ScmDoraService.class);

    }

    @Bean
    DoraService doraService() {
        return Mockito.mock(DoraService.class);
    }

    @Bean
    ADODoraService adoDoraService() {
        return Mockito.mock(ADODoraService.class);
    }

    @Bean
    AggCacheService aggCacheService() {
        return Mockito.mock(AggCacheService.class);

    }

    @Bean
    LegacyLeadTimeCalculationService legacyLeadTimeCalculation() {
        return Mockito.mock(LegacyLeadTimeCalculationService.class);
    }

    @Bean
    ObjectMapper mapper() {
        return Mockito.mock(ObjectMapper.class);

    }

    @Bean
    VelocityConfigsService velocityConfigsService() {
        return Mockito.mock(VelocityConfigsService.class);
    }

    @Bean
    VelocityAggsService velocityAggsService() {
        return Mockito.mock(VelocityAggsService.class);
    }

    @Bean
    CiCdAggsService cicdAggsService() {
        return Mockito.mock(CiCdAggsService.class);
    }

    @Bean
    ConfigTableHelper configTableHelper() {
        return Mockito.mock(ConfigTableHelper.class);
    }

    @Bean
    JiraIssueService jiraIssueService() {
        return Mockito.mock(JiraIssueService.class);
    }

    @Bean
    ScmAggService scmAggService() {
        return Mockito.mock(ScmAggService.class);
    }

    @Bean
    EsScmPRsService esScmPRsService() {
        return Mockito.mock(EsScmPRsService.class);
    }

    @Bean
    TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService() {
        return Mockito.mock(TicketCategorizationSchemeDatabaseService.class);
    }

    @Bean
    IngestedAtCachingService ingestedAtCachingService() {
        return Mockito.mock(IngestedAtCachingService.class);
    }

    @Bean
    EsWorkItemsQueryService esWorkItemsQueryService() {
        return Mockito.mock(EsWorkItemsQueryService.class);
    }

    @Bean
    WorkItemsService workItemsService() {
        return Mockito.mock(WorkItemsService.class);
    }

    @Bean
    EsJiraIssueQueryService esJiraIssueQueryService() {
        return Mockito.mock(EsJiraIssueQueryService.class);
    }

    @Bean
    DevProductivityOpsService devProductivityOpsService() { return Mockito.mock(DevProductivityOpsService.class); }

    @Bean
    OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService() { return Mockito.mock(OrgUnitCategoryDatabaseService.class); }

    @Bean
    GenericRequestsClient genericRequestsClient() { return Mockito.mock(GenericRequestsClient.class); }

    @Bean
    JiraSprintMetricsServiceLegacy jiraSprintMetricsServiceLegacy() {
        return Mockito.mock(JiraSprintMetricsServiceLegacy.class);
    }

    @Bean
    JiraSprintMetricsService jiraSprintMetricsService() {
        return Mockito.mock(JiraSprintMetricsService.class);
    }

    @Bean
    Executor dbValuesTaskExecutor() {
        return Mockito.mock(Executor.class);
    }

    @Bean
    AtlassianConnectServiceClient atlassianConnectServiceClient() {
        return Mockito.mock(AtlassianConnectServiceClient.class);
    }

    @Bean
    JiraFieldConditionsBuilder jiraFieldConditionsBuilder() {
        return Mockito.mock(JiraFieldConditionsBuilder.class);
    }

    @Bean
    JiraFieldService jiraFieldService() {
        return Mockito.mock(JiraFieldService.class);
    }

    @Bean
    JiraIssueApiService jiraIssueApiService() {
        return Mockito.mock(JiraIssueApiService.class);
    }

    @Bean
    VelocityStageTimesReportPrecalculateWidgetService velocityStageTimesReportPrecalculateWidgetService() {
        return Mockito.mock(VelocityStageTimesReportPrecalculateWidgetService.class);
    }

    @Bean
    GcpLoggingService gcpLoggingService() {
        return Mockito.mock(GcpLoggingService.class);
    }

    @Bean
    Clock clock() {
        return Mockito.mock(Clock.class);
    }


}
