package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.services.AWSDevToolsBuildBatchDatabaseService;
import io.levelops.commons.databases.services.AWSDevToolsBuildDatabaseService;
import io.levelops.commons.databases.services.AWSDevToolsProjectDatabaseService;
import io.levelops.commons.databases.services.AccessKeyService;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.AdminActivityLogService;
import io.levelops.commons.databases.services.AdminUserService;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.AiReportDatabaseService;
import io.levelops.commons.databases.services.BestPracticesService;
import io.levelops.commons.databases.services.BitbucketRepositoryService;
import io.levelops.commons.databases.services.CiCdAggsService;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobConfigChangesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunArtifactMappingDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunArtifactsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunDetailsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdPushedArtifactsDatabaseService;
import io.levelops.commons.databases.services.CiCdPushedParamsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.CiCdPipelinesAggsService;
import io.levelops.commons.databases.services.CiCdPreProcessTaskService;
import io.levelops.commons.databases.services.CiCdScmCombinedAggsService;
import io.levelops.commons.databases.services.CiCdScmMappingService;
import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService.CicdArtifactCorrelationSettings;
import io.levelops.commons.databases.services.ComponentProductMappingService;
import io.levelops.commons.databases.services.ComponentsDatabaseService;
import io.levelops.commons.databases.services.ConfigTableDatabaseService;
import io.levelops.commons.databases.services.ContentSchemaDatabaseService;
import io.levelops.commons.databases.services.CoverityDatabaseService;
import io.levelops.commons.databases.services.CxSastAggService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.EventTypesDatabaseService;
import io.levelops.commons.databases.services.GenericEventDatabaseService;
import io.levelops.commons.databases.services.GerritRepositoryService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.GithubAggService;
import io.levelops.commons.databases.services.IntegrationAggService;
import io.levelops.commons.databases.services.IntegrationSecretMappingsDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.IssueMgmtProjectService;
import io.levelops.commons.databases.services.IssueMgmtSprintMappingDatabaseService;
import io.levelops.commons.databases.services.IssuesMilestoneService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.JiraIssueSprintMappingDatabaseService;
import io.levelops.commons.databases.services.JiraIssueStoryPointsDatabaseService;
import io.levelops.commons.databases.services.JiraOktaService;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.databases.services.JiraSalesforceService;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.KudosDatabaseService;
import io.levelops.commons.databases.services.MsTMTDatabaseService;
import io.levelops.commons.databases.services.MsgTemplateService;
import io.levelops.commons.databases.services.OUDashboardService;
import io.levelops.commons.databases.services.OktaAggService;
import io.levelops.commons.databases.services.OrganizationService;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.commons.databases.services.PluginResultsDatabaseService;
import io.levelops.commons.databases.services.ProductIntegMappingService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.QuestionnaireDBService;
import io.levelops.commons.databases.services.QuestionnaireNotificationsDatabaseService;
import io.levelops.commons.databases.services.QuestionnaireTemplateDBService;
import io.levelops.commons.databases.services.RunbookDatabaseService;
import io.levelops.commons.databases.services.RunbookNodeTemplateDatabaseService;
import io.levelops.commons.databases.services.RunbookReportDatabaseService;
import io.levelops.commons.databases.services.RunbookReportSectionDatabaseService;
import io.levelops.commons.databases.services.RunbookRunDatabaseService;
import io.levelops.commons.databases.services.RunbookRunningNodeDatabaseService;
import io.levelops.commons.databases.services.RunbookTemplateCategoryDatabaseService;
import io.levelops.commons.databases.services.RunbookTemplateDatabaseService;
import io.levelops.commons.databases.services.SalesforceCaseService;
import io.levelops.commons.databases.services.SamlConfigService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmDoraAggService;
import io.levelops.commons.databases.services.ScmIssueMgmtService;
import io.levelops.commons.databases.services.ScmJiraZendeskService;
import io.levelops.commons.databases.services.ScmWebhooksDataService;
import io.levelops.commons.databases.services.SectionsService;
import io.levelops.commons.databases.services.ServicesDatabaseService;
import io.levelops.commons.databases.services.SlackTenantLookupDatabaseService;
import io.levelops.commons.databases.services.SlackUsersDatabaseService;
import io.levelops.commons.databases.services.SonarQubeIssueService;
import io.levelops.commons.databases.services.SonarQubeProjectService;
import io.levelops.commons.databases.services.StateDBService;
import io.levelops.commons.databases.services.StoredFiltersService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.databases.services.TenantStateService;
import io.levelops.commons.databases.services.TestRailsCaseFieldDatabaseService;
import io.levelops.commons.databases.services.TestRailsProjectDatabaseService;
import io.levelops.commons.databases.services.TestRailsTestCaseDatabaseService;
import io.levelops.commons.databases.services.TestRailsTestPlanDatabaseService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.TicketTemplateDBService;
import io.levelops.commons.databases.services.TokenDataService;
import io.levelops.commons.databases.services.TriageRuleHitsService;
import io.levelops.commons.databases.services.TriageRulesService;
import io.levelops.commons.databases.services.TriggerSchemasDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.WorkItemDBService;
import io.levelops.commons.databases.services.WorkItemFailureTriageViewService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemNotesService;
import io.levelops.commons.databases.services.WorkItemNotificationsDatabaseService;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsMetadataService;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.WorkflowDatabaseService;
import io.levelops.commons.databases.services.WorkflowPolicyDatabaseService;
import io.levelops.commons.databases.services.ZendeskFieldService;
import io.levelops.commons.databases.services.ZendeskTicketService;
import io.levelops.commons.databases.services.atlassian_connect.AtlassianConnectDatabaseService;
import io.levelops.commons.databases.services.automation_rules.AutomationRuleHitsDatabaseService;
import io.levelops.commons.databases.services.automation_rules.AutomationRulesDatabaseService;
import io.levelops.commons.databases.services.blackduck.BlackDuckDatabaseService;
import io.levelops.commons.databases.services.bullseye.BullseyeDatabaseService;
import io.levelops.commons.databases.services.dev_productivity.*;
import io.levelops.commons.databases.services.organization.OrgAccessValidationService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.databases.services.organization.WorkspaceDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyAlertsDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyIncidentsDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyServicesDatabaseService;
import io.levelops.commons.databases.services.precalculation.WidgetPrecalculatedResultsDBService;
import io.levelops.commons.databases.services.scm.ScmCommitPullRequestMappingDBService;
import io.levelops.commons.databases.services.snyk.SnykDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityAggsDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.databases.services.BAProfileDatabaseService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DatabaseSetupControllerInjectionTest.Config.class})
public class DatabaseSetupControllerInjectionTest {

    @Autowired
    List<DatabaseService<?>> databaseServiceList;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private UserService userService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private TokenDataService tokenDataService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private GitRepositoryService gitRepositoryService;
    @Autowired
    private BitbucketRepositoryService bitbucketRepositoryService;
    @Autowired
    private SamlConfigService samlConfigService;
    @Autowired
    private JiraProjectService jiraProjectService;
    @Autowired
    private DashboardWidgetService dashboardWidgetService;
    @Autowired
    private WorkItemDBService workItemDBService;
    @Autowired
    private ActivityLogService activityLogService;
    @Autowired
    private WorkItemNotesService workItemNotesService;
    @Autowired
    private AccessKeyService accessKeyService;
    @Autowired
    private QuestionnaireDBService questionnaireDbService;
    @Autowired
    private QuestionnaireTemplateDBService questionnaireTemplateDbService;
    @Autowired
    private StateDBService stateDBService;
    @Autowired
    private SectionsService sectionsService;
    @Autowired
    private TagsService tagsService;
    @Autowired
    private TagItemDBService tagItemService;
    @Autowired
    private MsgTemplateService msgTemplateService;
    @Autowired
    private BestPracticesService bestPracticesService;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductIntegMappingService productIntegMappingService;
    @Autowired
    private JiraFieldService jiraFieldService;
    @Autowired
    private PluginDatabaseService pluginDatabaseService;
    @Autowired
    private PluginResultsDatabaseService pluginResultsDatabaseService;
    @Autowired
    private IntegrationAggService integrationAggService;
    @Autowired
    private AggregationsDatabaseService aggregationsDatabaseService;
    @Autowired
    private WorkflowDatabaseService workflowDatabaseService;
    @Autowired
    private WorkflowPolicyDatabaseService workflowPolicyDatabaseService;
    @Autowired
    private RunbookNodeTemplateDatabaseService runbookNodeTemplateDatabaseService;
    @Autowired
    private RunbookDatabaseService runbookDatabaseService;
    @Autowired
    private RunbookRunDatabaseService runbookRunDatabaseService;
    @Autowired
    private RunbookRunningNodeDatabaseService runbookRunningNodeDatabaseService;
    @Autowired
    private ComponentProductMappingService mappingService;
    @Autowired
    private TicketTemplateDBService ticketTemplateDBService;
    @Autowired
    private EventTypesDatabaseService eventTypeDBService;
    @Autowired
    private ScmAggService scmAggService;
    @Autowired
    private ComponentsDatabaseService componentsDatabaseService;
    @Autowired
    private TriggerSchemasDatabaseService triggerSchemasDatabaseService;
    @Autowired
    private RunbookReportDatabaseService runbookReportDatabaseService;
    @Autowired
    private RunbookReportSectionDatabaseService runbookReportSectionDatabaseService;
    @Autowired
    private TenantConfigService tenantConfigService;
    @Autowired
    private JiraIssueService jiraIssueService;
    @Autowired
    private ContentSchemaDatabaseService contentSchemaDatabaseService;
    @Autowired
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    @Autowired
    private CiCdJobsDatabaseService ciCdJobsDatabaseService;
    @Autowired
    private CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    @Autowired
    private CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService;
    @Autowired
    private CiCdJobRunTestDatabaseService ciCdJobRunTestDatabaseService;
    @Autowired
    private CiCdScmMappingService ciCdScmMappingService;
    @Autowired
    private CiCdAggsService ciCdAggsService;
    @Autowired
    private CiCdScmCombinedAggsService ciCdScmCombinedAggsService;
    @Autowired
    private CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService;
    @Autowired
    private ZendeskTicketService zendeskTicketService;
    @Autowired
    private SalesforceCaseService salesforceCaseService;
    @Autowired
    private CiCdPipelinesAggsService ciCdPipelinesAggsService;
    @Autowired
    private IntegrationTrackingService trackingService;
    @Autowired
    private GerritRepositoryService gerritRepositoryService;
    @Autowired
    private ScmJiraZendeskService scmJiraZendeskService;
    @Autowired
    private ConfigTableDatabaseService configTableDatabaseService;
    @Autowired
    private CiCdJobRunDetailsDatabaseService ciCdJobRunDetailsDatabaseService;
    @Autowired
    private CiCdJobRunStageDatabaseService stagesService;
    @Autowired
    private TriageRulesService triageRulesService;
    @Autowired
    private TriageRuleHitsService triageRuleHitsService;
    @Autowired
    private StoredFiltersService storedFiltersService;
    @Autowired
    private OktaAggService oktaAggService;
    @Autowired
    private JiraOktaService jiraOktaService;
    @Autowired
    private CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    @Autowired
    private JiraSalesforceService jiraSalesforceService;
    @Autowired
    private PagerDutyAlertsDatabaseService pagerdutyAlertsService;
    @Autowired
    private PagerDutyIncidentsDatabaseService pagerdutyIncidentsService;
    @Autowired
    private PagerDutyServicesDatabaseService pagerdutyServicesService;
    @Autowired
    private ServicesDatabaseService servicesDbService;
    @Autowired
    private WorkItemNotificationsDatabaseService workItemNotificationsDatabaseService;
    @Autowired
    private QuestionnaireNotificationsDatabaseService questionnaireNotificationsDatabaseService;
    @Autowired
    private WorkItemFailureTriageViewService workItemFailureTriageViewService;
    @Autowired
    private SlackTenantLookupDatabaseService slackTenantLookupDatabaseService;
    @Autowired
    private SlackUsersDatabaseService slackUsersDatabaseService;
    @Autowired
    private TestRailsProjectDatabaseService testRailsProjectDatabaseService;
    @Autowired
    private TestRailsTestPlanDatabaseService testRailsTestPlanDatabaseService;
    @Autowired
    private TestRailsCaseFieldDatabaseService testRailsCaseFieldDatabaseService;
    @Autowired
    private SonarQubeIssueService sonarQubeIssueService;
    @Autowired
    private SonarQubeProjectService sonarQubeProjectService;
    @Autowired
    private io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService CiCdJobRunTestDatabaseService;
    @Autowired
    private AutomationRulesDatabaseService automationRulesDatabaseService;
    @Autowired
    private AutomationRuleHitsDatabaseService automationRuleHitsDatabaseService;
    @Autowired
    private CxSastAggService cxSastAggService;
    @Autowired
    private RunbookTemplateDatabaseService runbookTemplateDatabaseService;
    @Autowired
    private RunbookTemplateCategoryDatabaseService runbookTemplateCategoryDatabaseService;
    @Autowired
    private AWSDevToolsProjectDatabaseService awsDevToolsProjectDatabaseService;
    @Autowired
    private AWSDevToolsBuildDatabaseService awsDevToolsBuildDatabaseService;
    @Autowired
    private AWSDevToolsBuildBatchDatabaseService awsDevToolsBuildBatchDatabaseService;
    @Autowired
    private SnykDatabaseService snykDatabaseService;
    @Autowired
    private BullseyeDatabaseService bullseyeDatabaseService;
    @Autowired
    private IntegrationSecretMappingsDatabaseService integrationSecretMappingsDatabaseService;
    @Autowired
    private MsTMTDatabaseService msTMTDatabaseService;
    @Autowired
    private ZendeskFieldService zendeskFieldService;
    @Autowired
    private TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    @Autowired
    private JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService;
    @Autowired
    private JiraIssueStoryPointsDatabaseService jiraIssueStoryPointsDatabaseService;
    @Autowired
    private JiraIssueSprintMappingDatabaseService jiraIssueSprintMappingDatabaseService;
    @Autowired
    private VelocityConfigsDatabaseService velocityConfigsDatabaseService;
    @Autowired
    private VelocityAggsDatabaseService velocityAggsDatabaseService;
    @Autowired
    private DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    @Autowired
    private DevProductivityParentProfileDatabaseService devProductivityParentProfileDatabaseService;
    @Autowired
    private UserDevProductivityESReportDatabaseService userDevProductivityESReportDatabaseService;
    @Autowired
    private OrgDevProductivityESReportDatabaseService orgDevProductivityESReportDatabaseService;
    @Autowired
    private GithubAggService githubAggService;
    @Autowired
    private ProductsDatabaseService productsDatabaseService;
    @Autowired
    private TeamsDatabaseService teamsDatabaseService;
    @Autowired
    private TeamMembersDatabaseService teamMembersDatabaseService;
    @Autowired
    private UserIdentityService userIdentityService;
    @Autowired
    private BlackDuckDatabaseService blackDuckDatabaseService;
    @Autowired
    private WorkItemFieldsMetaService workItemFieldsMetaService;
    @Autowired
    private WorkItemsService workItemsService;
    @Autowired
    private WorkItemsPrioritySLAService workItemsPrioritySLAService;
    @Autowired
    private WorkItemTimelineService workItemTimelineService;
    @Autowired
    private CoverityDatabaseService coverityDatabaseService;
    @Autowired
    private IssueMgmtProjectService issueMgmtProjectService;
    @Autowired
    private IssuesMilestoneService issuesMilestoneService;
    @Autowired
    private IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService;
    @Autowired
    private WorkItemsMetadataService workItemsMetadataService;
    @Autowired
    private ScmIssueMgmtService scmIssueMgmtService;
    @Autowired
    private OrgVersionsDatabaseService orgVersionsService;
    @Autowired
    private OrgUsersDatabaseService orgUsersService;
    @Autowired
    private OrgUnitsDatabaseService orgUnitsDatabaseService;
    @Autowired
    private UserDevProductivityReportDatabaseService userDevProductivityReportDatabaseService;
    @Autowired
    private UserDevProductivityReportV2DatabaseService userDevProductivityReportV2DatabaseService;
    @Autowired
    private OrgDevProductivityReportDatabaseService orgDevProductivityReportDatabaseService;
    @Autowired
    private OrgDevProductivityReportV2DatabaseService orgDevProductivityReportV2DatabaseService;
    @Autowired
    private OrgDevProductivityReportV3DatabaseService orgDevProductivityReportV3DatabaseService;
    @Autowired
    private GlobalTrackersDatabaseService globalTrackersDatabaseService;
    @Autowired
    private DevProductivityRelativeScoreService devProductivityRelativeScoreService;
    @Autowired
    private OrgAndUsersDevProductivityReportMappingsDBService orgAndUsersDevProductivityReportMappingsDBService;
    @Autowired
    private OrgAndUsersDevProductivityReportMappingsV2DBService orgAndUsersDevProductivityReportMappingsV2DBService;
    @Autowired
    private OrgAndUsersDevProductivityReportMappingsV3DBService orgAndUsersDevProductivityReportMappingsV3DBService;
    @Autowired
    private IndustryDevProductivityReportDatabaseService industryDevProductivityReportDatabaseService;
    @Autowired
    private ScmWebhooksDataService scmWebhooksDataService;
    @Autowired
    private KudosDatabaseService kudosDataService;
    @Autowired
    private AdminUserService adminUserService;
    @Autowired
    private ScmDoraAggService scmDoraAggService;
    @Autowired
    private TenantStateService tenantStateService;
    @Autowired
    private AdminActivityLogService adminActivityLogService;
    @Autowired
    private OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    @Autowired
    private OUDashboardService ouDashboardService;
    @Autowired
    private OUOrgUserMappingDatabaseService ouOrgUserMappingDatabaseService;
    @Autowired
    private OUOrgUserMappingV2DatabaseService ouOrgUserMappingV2DatabaseService;
    @Autowired
    private ScmCommitPullRequestMappingDBService scmCommitPullRequestMappingDBService;
    @Autowired
    private WorkspaceDatabaseService workspaceDatabaseService;
    @Autowired
    private WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService;
    @Autowired
    private GenericEventDatabaseService genericEventDatabaseService;
    @Autowired
    private CiCdPreProcessTaskService ciCdPreProcessTaskService;

    @Autowired
    private AtlassianConnectDatabaseService atlassianConnectDatabaseService;
    @Autowired
    private OrgProfileDatabaseService orgProfileDatabaseService;
    @Autowired
    private CiCdJobRunArtifactMappingDatabaseService ciCdJobRunArtifactMappingDatabaseService;
    @Autowired
    private CiCdPushedArtifactsDatabaseService ciCdPushedArtifactsDatabaseService;
    @Autowired
    private CiCdPushedParamsDatabaseService ciCdPushedParamsDatabaseService;
    @Autowired
    private AiReportDatabaseService aiReportDatabaseService;
    @Autowired
    private TestRailsTestCaseDatabaseService testRailsTestCaseDatabaseService;
    @Autowired
    private OrgUserDevProductivityProfileMappingDatabaseService orgUserDevProductivityProfileMappingDatabaseService;
    @Autowired
    private BAProfileDatabaseService baProfileDatabaseService;

    @SuppressWarnings("unchecked")
    private static Set<Class<? extends DatabaseService<?>>> classesOf(DatabaseService<?>... databaseServices) {
        //noinspection unchecked
        return Stream.of(databaseServices)
                .map(s -> (Class<? extends DatabaseService<?>>) s.getClass())
                .collect(Collectors.toSet());
    }

    @Test
    public void testGlobalTables() {
        assertThat(databaseServiceList.stream()
                .filter(s -> !s.isTenantSpecific())
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        tenantService,
                        slackTenantLookupDatabaseService,
                        industryDevProductivityReportDatabaseService,
                        adminUserService,
                        adminActivityLogService,
                        ciCdPreProcessTaskService,
                        atlassianConnectDatabaseService);
    }

    @Test
    public void testTenantTables() {
        assertThat(databaseServiceList.stream()
                .filter(DatabaseService::isTenantSpecific)
                .peek(s -> System.out.println(s.getClass().getSimpleName()))
                .count()).isEqualTo(155);
    }

    @Test
    public void testDependencyGraph() {
        Map<Set<Class<? extends DatabaseService<?>>>, Set<DatabaseService<?>>> graph = DatabaseSetupController.buildDependencyGraph(databaseServiceList);
        Set<Set<Class<? extends DatabaseService<?>>>> t = new HashSet<>();

        System.out.println();
        System.out.println(graph);
        System.out.println("\nFormat:              [ dependencies (requirements) ] --> [ dependents ]");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------");

        // verify that the right dependencies (set of classes) have the right dependents (set of services)
        // for example: { A, B } -> { C , D } means that classes C and D (dependents) require classes A and B to be initialized first (dependencies)
        verifyGraphContains(t, graph, Set.of(), Set.of(componentsDatabaseService, triggerSchemasDatabaseService, stateDBService,
                userService, msgTemplateService, pluginDatabaseService, integrationService, activityLogService, samlConfigService,
                organizationService, sectionsService, tagsService, bestPracticesService, workflowDatabaseService,
                workflowPolicyDatabaseService, accessKeyService, runbookDatabaseService, runbookNodeTemplateDatabaseService,
                runbookReportDatabaseService, contentSchemaDatabaseService, configTableDatabaseService,
                triageRulesService, storedFiltersService, servicesDbService,
                workItemFailureTriageViewService, slackUsersDatabaseService, automationRulesDatabaseService,
                runbookTemplateDatabaseService, runbookTemplateCategoryDatabaseService, ticketCategorizationSchemeDatabaseService,
                scmIssueMgmtService, orgVersionsService, globalTrackersDatabaseService, devProductivityRelativeScoreService, scmDoraAggService, genericEventDatabaseService,
                orgProfileDatabaseService, aiReportDatabaseService, baProfileDatabaseService));
        verifyGraphContains(t, graph, classesOf(dashboardWidgetService), Set.of(tenantConfigService, kudosDataService, tenantStateService));
        verifyGraphContains(t, graph, classesOf(tagsService), Set.of(tagItemService));
        verifyGraphContains(t, graph, classesOf(orgUsersService, dashboardWidgetService, orgUnitCategoryDatabaseService), Set.of(orgUnitsDatabaseService));
        verifyGraphContains(t, graph, classesOf(workspaceDatabaseService, productService), Set.of(orgUnitCategoryDatabaseService));
        verifyGraphContains(t, graph, classesOf(orgVersionsService, integrationService, userIdentityService), Set.of(orgUsersService));
        verifyGraphContains(t, graph, classesOf(triageRulesService, ciCdJobRunsDatabaseService, ciCdJobRunStageStepsDatabaseService, stagesService), Set.of(triageRuleHitsService));

        verifyGraphContains(t, graph, classesOf(productService), Set.of(integrationAggService, aggregationsDatabaseService, mappingService));
        verifyGraphContains(t, graph, classesOf(teamMembersDatabaseService), Set.of(teamsDatabaseService));
        verifyGraphContains(t, graph, classesOf(integrationService, userIdentityService), Set.of(scmAggService, teamMembersDatabaseService, workItemsService));
        verifyGraphContains(t, graph, classesOf(integrationService), Set.of(trackingService, jiraProjectService, gitRepositoryService,
                bitbucketRepositoryService, jiraFieldService, tokenDataService, zendeskTicketService, zendeskFieldService,
                salesforceCaseService, gerritRepositoryService, oktaAggService, testRailsProjectDatabaseService,
                testRailsTestPlanDatabaseService, testRailsCaseFieldDatabaseService, testRailsTestCaseDatabaseService, sonarQubeProjectService,
                awsDevToolsProjectDatabaseService, awsDevToolsBuildDatabaseService, awsDevToolsBuildBatchDatabaseService, snykDatabaseService,
                integrationSecretMappingsDatabaseService, cxSastAggService, ciCdInstancesDatabaseService, jiraStatusMetadataDatabaseService,
                jiraIssueStoryPointsDatabaseService, jiraIssueSprintMappingDatabaseService, productsDatabaseService, coverityDatabaseService,
                userIdentityService, workItemTimelineService, issueMgmtProjectService, workItemFieldsMetaService,
                issueMgmtSprintMappingDatabaseService, workItemsMetadataService, blackDuckDatabaseService, workItemsPrioritySLAService, scmWebhooksDataService, workspaceDatabaseService,
                ciCdPushedArtifactsDatabaseService, ciCdPushedParamsDatabaseService));
        verifyGraphContains(t, graph, classesOf(integrationService, workItemsService), Set.of(issuesMilestoneService));
        verifyGraphContains(t, graph, classesOf(userService), Set.of(productService, dashboardWidgetService));
        // verifyGraphContains(t, graph, classesOf(jiraFieldService), Set.of(jiraIssueService));
        verifyGraphContains(t, graph, classesOf(jiraFieldService, integrationService), Set.of(jiraIssueService));
        verifyGraphContains(t, graph, classesOf(mappingService), Set.of(pluginResultsDatabaseService));
        verifyGraphContains(t, graph, classesOf(questionnaireTemplateDbService), Set.of(ticketTemplateDBService));
        verifyGraphContains(t, graph, classesOf(integrationService, userService, productService, stateDBService, ticketTemplateDBService, ciCdJobRunsDatabaseService, stagesService), Set.of(workItemDBService));
        verifyGraphContains(t, graph, classesOf(workItemDBService), Set.of(workItemNotesService, workItemNotificationsDatabaseService));
        verifyGraphContains(t, graph, classesOf(productService, integrationService), Set.of(productIntegMappingService));
        verifyGraphContains(t, graph, classesOf(workItemDBService, questionnaireTemplateDbService, productService, bestPracticesService), Set.of(questionnaireDbService));
        verifyGraphContains(t, graph, classesOf(runbookDatabaseService), Set.of(runbookRunDatabaseService));
        verifyGraphContains(t, graph, classesOf(runbookRunDatabaseService), Set.of(runbookRunningNodeDatabaseService));
        verifyGraphContains(t, graph, classesOf(componentsDatabaseService), Set.of(eventTypeDBService));
        verifyGraphContains(t, graph, classesOf(runbookReportDatabaseService), Set.of(runbookReportSectionDatabaseService));
        verifyGraphContains(t, graph, classesOf(ciCdJobsDatabaseService), Set.of(ciCdJobRunsDatabaseService, ciCdJobConfigChangesDatabaseService, velocityConfigsDatabaseService));
        verifyGraphContains(t, graph, classesOf(ciCdJobRunsDatabaseService, scmAggService), Set.of(ciCdScmMappingService));
        verifyGraphContains(t, graph, classesOf(ciCdJobsDatabaseService, ciCdJobRunsDatabaseService, ciCdScmMappingService, scmAggService), Set.of(ciCdScmCombinedAggsService));
        verifyGraphContains(t, graph, classesOf(ciCdJobsDatabaseService, ciCdJobRunsDatabaseService), Set.of(ciCdAggsService, ciCdPipelinesAggsService));
        verifyGraphContains(t, graph, classesOf(ciCdInstancesDatabaseService, productService), Set.of(ciCdJobsDatabaseService));
        verifyGraphContains(t, graph, classesOf(bestPracticesService), Set.of(questionnaireTemplateDbService));
        verifyGraphContains(t, graph, classesOf(jiraIssueService, scmAggService), Set.of(scmJiraZendeskService));
        verifyGraphContains(t, graph, classesOf(ciCdJobRunsDatabaseService), Set.of(ciCdJobRunDetailsDatabaseService, stagesService, ciCdJobRunTestDatabaseService,
                bullseyeDatabaseService, ciCdJobRunArtifactsDatabaseService, ciCdJobRunArtifactMappingDatabaseService));
        verifyGraphContains(t, graph, classesOf(triageRulesService, stagesService, ciCdJobRunsDatabaseService, ciCdJobRunStageStepsDatabaseService), Set.of(triageRuleHitsService));
        verifyGraphContains(t, graph, classesOf(jiraIssueService, oktaAggService), Set.of(jiraOktaService));
        verifyGraphContains(t, graph, classesOf(stagesService), Set.of(ciCdJobRunStageStepsDatabaseService));
        verifyGraphContains(t, graph, classesOf(jiraIssueService, salesforceCaseService), Set.of(jiraSalesforceService));
        verifyGraphContains(t, graph, classesOf(servicesDbService, integrationService), Set.of(pagerdutyServicesService));
        verifyGraphContains(t, graph, classesOf(pagerdutyServicesService, pagerdutyIncidentsService), Set.of(pagerdutyAlertsService));
        verifyGraphContains(t, graph, classesOf(pagerdutyServicesService), Set.of(pagerdutyIncidentsService));
        verifyGraphContains(t, graph, classesOf(questionnaireDbService), Set.of(questionnaireNotificationsDatabaseService));
        verifyGraphContains(t, graph, classesOf(sonarQubeProjectService), Set.of(sonarQubeIssueService));
        verifyGraphContains(t, graph, classesOf(automationRulesDatabaseService), Set.of(automationRuleHitsDatabaseService));
        verifyGraphContains(t, graph, classesOf(pluginResultsDatabaseService), Set.of(msTMTDatabaseService));
        verifyGraphContains(t, graph, classesOf(jiraIssueService, scmAggService, ciCdScmMappingService,
                ciCdInstancesDatabaseService, ciCdJobsDatabaseService, ciCdJobRunsDatabaseService), Set.of(velocityAggsDatabaseService));
        verifyGraphContains(t, graph, classesOf(scmAggService), Set.of(githubAggService, scmCommitPullRequestMappingDBService));
        verifyGraphContains(t, graph, classesOf(ticketCategorizationSchemeDatabaseService), Set.of(devProductivityProfileDatabaseService));
        verifyGraphContains(t, graph, classesOf(ticketCategorizationSchemeDatabaseService,devProductivityProfileDatabaseService), Set.of(devProductivityParentProfileDatabaseService));
        verifyGraphContains(t, graph, classesOf(orgUnitsDatabaseService, devProductivityProfileDatabaseService),
                Set.of(userDevProductivityReportDatabaseService, userDevProductivityESReportDatabaseService, orgDevProductivityReportDatabaseService, orgDevProductivityESReportDatabaseService, orgAndUsersDevProductivityReportMappingsDBService, orgDevProductivityReportV2DatabaseService, orgAndUsersDevProductivityReportMappingsV2DBService, orgAndUsersDevProductivityReportMappingsV3DBService));
        verifyGraphContains(t, graph, classesOf(orgUnitsDatabaseService, devProductivityParentProfileDatabaseService),
                Set.of(orgDevProductivityReportV3DatabaseService));
        verifyGraphContains(t, graph, classesOf(orgUnitsDatabaseService, devProductivityProfileDatabaseService,  orgUsersService),
                Set.of(userDevProductivityReportV2DatabaseService));
        verifyGraphContains(t, graph, classesOf(orgUnitsDatabaseService, orgUsersService), Set.of(ouOrgUserMappingDatabaseService, ouOrgUserMappingV2DatabaseService, orgUserDevProductivityProfileMappingDatabaseService));
        verifyGraphContains(t, graph, classesOf(orgUnitsDatabaseService, dashboardWidgetService), Set.of(ouDashboardService, widgetPrecalculatedResultsDBService));

        // ensures all the tests have been written
        assertThat(t).containsExactlyInAnyOrderElementsOf(graph.keySet());
        // number of requirement sets
        assertThat(graph).hasSize(51);
    }

    private void verifyGraphContains(Set<Set<Class<? extends DatabaseService<?>>>> testedReqs,
                                     Map<Set<Class<? extends DatabaseService<?>>>, Set<DatabaseService<?>>> graph,
                                     Set<Class<? extends DatabaseService<?>>> dependencies,
                                     Set<DatabaseService<?>> dependents) {
        assertThat(graph).as("Graph is missing dependencies entry (references)").containsKey(dependencies);
        assertThat(graph.get(dependencies)).as("Dependents missing for : " + dependencies.toString()).containsExactlyInAnyOrderElementsOf(dependents);
        System.out.println(String.format("%52s --> %s",
                IterableUtils.parseIterable(dependencies, Class::getSimpleName),
                IterableUtils.parseIterable(dependents, c -> c.getClass().getSimpleName())));
        testedReqs.add(dependencies);
    }

    @ComponentScan("io.levelops.commons.databases.services")
    public static class Config {
        @Bean
        public DataSource dataSource() {
            return Mockito.mock(DataSource.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            return DefaultObjectMapper.get();
        }

        @Bean
        public OrgAccessValidationService orgAccessValidationService() {
            return Mockito.mock(OrgAccessValidationService.class);
        }

        @Bean
        public OrgUnitHelper orgUnitHelper() {
            return Mockito.mock(OrgUnitHelper.class);
        }

        @Bean
        public CicdArtifactCorrelationSettings cicdArtifactCorrelationSettings() {
            return CicdArtifactCorrelationSettings.builder().build();
        }
    }
}
