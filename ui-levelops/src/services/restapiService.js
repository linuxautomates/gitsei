import * as services from "./restapi";
import * as RestURI from "constants/restUri";
import { OrganizationUnitsForIntegrationService, RestDevProdUserScoreReportService } from "./restapi";

export default class RestapiService {
  constructor() {
    this.activitylogs = new services.RestActivitylogsService();
    this.apikeys = new services.RestApikeysService();
    this.assessment_download = new services.AssessmentsDownloadService();
    this.assignee_time_report = new services.AssigneeTimeReportService();
    this.automation_rules = new services.RestAutomationRules();
    this.bestpractices = new services.RestBestpracticesService();
    this.bounce_report = new services.BounceReportService();
    this.cicd_filter_values = new services.CICDFiltersService();
    this.cicd_job_aggs = new services.CICDJobsAggService();
    this.cicd_job_stages = new services.CICDJobsStagesService();
    this.cicd_jobs = new services.CICDJobNamesService();
    this.cicd_scm = new services.CICDService();
    this.cicd_scm_job_runs_tickets = new services.CICDJobsRunsTicketsService();
    this.cicd_users = new services.CICDUsersService();
    this.cicd_jobs_list = new services.CICDJobsService();
    this.comms = new services.RestCommsService();
    this.config_tables = new services.ConfigTablesService();
    this.configs = new services.ConfigService();
    this.configure_dashboard = new services.ConfigureDashService();
    this.content_schema = new services.RestContentSchema();
    this.ctemplates = new services.RestCtemplatesService();
    this.custom_fields = new services.RestCustomFieldsService();
    this.dashboard_reports = new services.DashboardReportsService();
    this.dashboards = new services.DashboardService();
    this.event_logs = new services.RestEventlogsService();
    this.fields = new services.RestFieldsService();
    this.files = new services.RestFileService();
    this.first_assignee_report = new services.FirstAssigneeReportService();
    this.github_commits_filter_values = new services.GithubCommitsFilterValueService();
    this.github_commits_report = new services.GithubCommitsReportService();
    this.github_commits_tickets = new services.GithubCommitsTicketsService();
    this.github_prs_filter_values = new services.GithubPRSFilterValueService();
    this.github_prs_report = new services.GithubPRSReportService();
    this.github_prs_author_response_time = new services.GithubPRsAuthorResponseTimeService();
    this.github_prs_reviewer_response_time = new services.GithubPRsReviewerResponseTimeService();
    this.github_prs_tickets = new services.GithubPRSTicketsService();
    this.github_coding_day = new services.GithubCodingDayReportService();
    this.github_commits_per_coding_day = new services.GithubCommitsPerCodingDayReportService();
    this.gitrepos = new services.RestGitreposService();
    this.hops_report = new services.HopsReportService();
    this.hygiene_report = new services.HygieneReportService();
    this.integrations = new services.RestIntegrationsService();
    this.jenkins_job_config_change_report = new services.JenkinsReportService();
    this.jenkins_job_config_change_tickets = new services.JenkinsJobConfigTicketsService();
    this.jenkins_job_config_filter_values = new services.JenkinsFilterValueService();
    this.jenkins_jobs_filter_values = new services.JenkinsJobsFilterValueService();
    this.jenkins_pipeline_job_runs = new services.JenkinsPipelinesJobsRunsService();
    this.jenkins_pipeline_job_stages = new services.JenkinsPipelinesJobStagesService();
    this.jenkins_pipeline_triage_runs = new services.JenkinsPipelinesJobsTriageService();
    this.jenkins_pipelines_jobs_filter_values = new services.JenkinsPipelinesJobsFilterValueService();
    this.jira_custom_filter_values = new services.JiraCustomFieldsValuesService();
    this.jira_fields = new services.JiraFieldsService();
    this.jira_filter_values = new services.JiraFilterValueService();
    this[RestURI.MICROSOFT_ISSUES_FILTER_VALUES] = new services.MicrosoftIssueFilterValueService();
    this[RestURI.MICROSOFT_ISSUES_REPORT] = new services.MicrosoftThreatModelingIssuesReportService();
    this[RestURI.MICROSOFT_ISSUES] = new services.MicrosoftIssuesService();
    this.jira_integration_config = new services.JiraCustomFieldsConfigService();
    this.jira_priorities = new services.RestJiraPrioritiesService();
    this.jira_salesforce = new services.JiraSalesforceService();
    this.jira_salesforce_aggs_list_commit = new services.JiraSalesforceAggsListCommitService();
    this.jira_salesforce_aggs_list_jira = new services.JiraSalesforceAggsListJiraService();
    this.jira_salesforce_aggs_list_salesforce = new services.JiraSalesforceAggsListSalesforceService();
    this.jira_tickets = new services.JiraTicketsService();
    this.jira_zendesk = new services.JiraZendeskService();
    this.jira_zendesk_aggs_list_commit = new services.JiraZendeskAggsListCommitService();
    this.jira_zendesk_aggs_list_jira = new services.JiraZendeskAggsListJiraService();
    this.jira_zendesk_aggs_list_zendesk = new services.JiraZendeskAggsListZendeskService();
    this.jira_time_across_stages_report = new services.JiraTimeAcrossStagesReportService();
    this.jiraprojects = new services.RestJiraprojectsService();
    this.jiraprojects_values = new services.JiraProjectsFilterValue();
    this.job_statuses = new services.JobStatusesService();
    this.jobs_change_volume_report = new services.JobsChangeVolumeReportService();
    this.jobs_commits_lead_cicd_report = new services.JobsCommitsLeadCICDReportService();
    this.jobs_commits_lead_report = new services.JobsCommitsLeadReportService();
    this.jobs_count_cicd_report = new services.JobsCountCICDReportService();
    this.jobs_count_report = new services.JobsCountReportService();
    this.jobs_duration_cicd_report = new services.JobsDurationCICDReportService();
    this.jobs_duration_report = new services.JobsDurationReportService();
    this.mappings = new services.RestMappingsService();
    this.metrics = new services.RestMetricsService();
    this.notes = new services.RestNotesService();
    this.objects = new services.RestObjectsService();
    this.pipeline_job_runs = new services.PipelineJobRunsService();
    this.pipelines_jobs_count_report = new services.PipelinesJobsCountReportService();
    this.pipelines_jobs_duration_report = new services.PipelinesJobsDurationReportService();
    this.propel_node_categories = new services.RestPropelNodeCategories();
    this.propel_node_templates = new services.RestPropelNodeTemplates();
    this.propel_reports = new services.RestPropelReports();
    this.propel_runs = new services.RestPropelRuns();
    this.propel_runs_logs = new services.PropelRunsLogsService();
    this.propel_trigger_events = new services.RestPropelTriggerEvents();
    this.propel_trigger_templates = new services.RestPropelTriggerTemplates();
    this.propels = new services.RestPropels();
    this.propels_nodes_evaluate = new services.PropelNodesEvaluateService();
    this.plugin_aggs = new services.RestPluginAggsService();
    this.plugin_labels = new services.RestPluginLabelsService();
    this.plugin_results = new services.RestPluginresultsService();
    this.plugins = new services.RestPluginsService();
    this.plugins_csv = new services.RestPluginsCSVService();
    this.policies = new services.RestPoliciesService();
    this.product_aggs = new services.RestProductAggsService();
    this.products = new services.RestProductsService();
    this.questionnaires = new services.RestQuestionnairesService();
    this.quiz = new services.RestQuizService();
    this.releases = new services.RestReleasesService();
    this.reports = new services.RestReportsService();
    this.repositories = new services.RestRepositoriesService();
    this.resolution_time_report = new services.ResolutionTimeReportService();
    this.response_time_report = new services.ResponseTimeReportService();
    this.salesforce_bounce_report = new services.SalesForceBounceReportService();
    this.salesforce_filter_values = new services.SalesForceFilterValuesService();
    this.salesforce_hops_report = new services.SalesForceHopsReportService();
    this.salesforce_hygiene_report = new services.SalesForceHygieneReportService();
    this.salesforce_resolution_time_report = new services.SalesForceResolutionTimeReportService();
    this.salesforce_response_time_report = new services.SalesForceResponseTimeReportService();
    this.salesforce_tickets = new services.SalesForceTicketsService();
    this.salesforce_tickets_report = new services.SalesForceTicketsReportService();
    this.samlsso = new services.RestSamlconfigService();
    this.scm_files_filter_values = new services.SCMFilesFilterValuesService();
    this.scm_files_report = new services.SCMFilesReportService();
    this.scm_files_root_folder_report = new services.SCMFilesReportRootFolderService();
    this.scm_issues_filter_values = new services.SCMIssuesFilterValuesService();
    this.scm_issues_first_response_report = new services.SCMIssuesFirstResponseService();
    this.scm_issues_report = new services.SCMIssuesReportService();
    this.scm_issues_tickets = new services.SCMIssuesTicketsService();
    this.scm_jira_files_report = new services.SCMJiraFilesReportService();
    this.scm_jira_files_root_folder_report = new services.SCMJiraFilesRootFolderReportService();
    this.scm_prs_first_review_to_merge_trend = new services.SCMPRSFirstReviewToMergeTrendService();
    this.scm_prs_first_review_trend = new services.SCMPRSFirstReviewTrendService();
    this.scm_prs_merge_trend = new services.SCMPRSMergeTrendReportService();
    this.scm_issues_time_across_stages = new services.SCMIssuesTimeAcrossStagesService();
    this.scm_issues_time_across_stages_filter_values = new services.SCMIssuesTimeAcrossStagesFilterValuesService();
    this.github_cards = new services.GithubCardsService();
    this.scm_users = new services.SCMUsersService();
    this.scm_rework_report = new services.SCMReworkReportService();
    this.sections = new services.RestSectionsService();
    this.services_report_aggregate = new services.ServicesReportAggregateService();
    this.services_report_aggregate_filter_values = new services.ServicesReportAggregateFiltersValueService();
    this.services_report_list = new services.ServicesReportWidgetService();
    this.signature_logs = new services.RestSignaturelogsService();
    this.signatures = new services.RestSignaturesService();
    this.stages = new services.RestStagesService();
    this.states = new services.RestStatesService();
    this.tags = new services.RestTagsService();
    this.ticket_templates = new services.RestSmartTicketTemplateService();
    this.tickets_report = new services.TicketsReportService();
    this.tools = new services.RestToolsService();
    this.triage_rule_results = new services.RestTriageRuleResultService();
    this.triage_rules = new services.RestTriageRulesService();
    this.users = new services.RestUsersService();
    this.widgets = new services.WidgetsService();
    this.workflows = new services.RestWorkflowsService();
    this.workitem = new services.RestWorkitemsService();
    this.zendesk_agent_wait_time_report = new services.ZendeskAgentWaitTimeReportService();
    this.zendesk_bounce_report = new services.ZendeskBounceReportService();
    this.zendesk_filter_values = new services.ZendeskFilterValuesService();
    this.zendesk_hops_report = new services.ZendeskHopsReportService();
    this.zendesk_hygiene_report = new services.ZendeskHygieneReportService();
    this.zendesk_reopens_report = new services.ZendeskReopensReportService();
    this.zendesk_replies_report = new services.ZendeskRepliesReportService();
    this.zendesk_requester_wait_time_report = new services.ZendeskRequesterWaitTimeReportService();
    this.zendesk_resolution_time_report = new services.ZendeskResolutionTimeReportService();
    this.zendesk_response_time_report = new services.ZendeskResponseTimeReportService();
    this.zendesk_tickets = new services.ZendeskTicketsService();
    this.zendesk_tickets_report = new services.ZendeskTicketsReportService();
    this.pipeline_job_runs_logs = new services.PipelineJobRunsLogService();
    this.pipeline_job_runs_stages_logs = new services.PipelineJobRunsStagesLogService();
    this.pagerduty_release_incidents = new services.PagerdutyReleaseIncidents();
    this.pagerduty_ack_trend = new services.PagerdutyAckTrend();
    this.pagerduty_after_hours = new services.PagerdutyAfterHours();
    this.pagerduty_filter_values = new services.PagerDutyFilterValues();
    this.questionnaires_notify = new services.QuestionnairesNotifyService();
    this.pagerduty_incident_rates = new services.PagerDutyIncidentRatesReport();
    this.pagerduty_incidents = new services.PagerDutyIncidentsService();
    this.pagerduty_alerts_aggs = new services.PagerDutyAlertsAggsService();
    this.pagerduty_incidents_aggs = new services.PagerDutyIncidentsAggsService();
    this.pagerduty_resolution_time_report = new services.PagerdutyResolutionTimeReportService();
    this.pagerduty_response_time_report = new services.PagerdutyResponseTimeReportService();
    this.jobs_run_tests = new services.JobRunTestsListService();
    this.jobs_run_tests_report = new services.JobRunTestsReportService();
    this.jobs_run_tests_filter_values = new services.JobRunTestsValuesService();
    this.jobs_run_tests_duration_report = new services.JobRunTestsDurationReportService();
    this.sonarqube_tickets = new services.SonarQubeReportService();
    this.sonarqube_issues_report = new services.SonarQubeIssuesReportService();
    this.sonarqube_effort_report = new services.SonarQubeEffortReportService();
    this.sonarqube_filter_values = new services.SonarQubeFilterValueService();
    this.testrails_tests_report = new services.TestrailsReportService();
    this.testrails_tests_list = new services.TestrailsListService();
    this.testrails_tests_values = new services.TestrailsValuesService();
    this.testrails_tests_estimate_report = new services.TestrailsEstimateReportService();
    this.testrails_tests_estimate_forecast_report = new services.TestrailsEstimateForecastReportService();
    this.sonarqube_metrics_list = new services.SonarQubeMetricsService();
    this.sonarqube_metrics_values = new services.SonarQubeMetricsValueService();
    this.sonarqube_metrics_report = new services.SonarQubeMetricsReportService();
    this.backlog_report = new services.BackogReportService();

    //levelops args reports
    this.quiz_aggs_count_report = new services.QuizAggsCountsReportsService();
    this.quiz_aggs_response_time_report = new services.QuizAggsResponseTimeReportsService();
    this.quiz_aggs_response_time_table_report = new services.QuizAggsReponseTimeTableReportService();
    this.work_item_aggs_count_report = new services.WorkItemAggsCountsReportsService();

    //jira salesforce
    this.jira_salesforce_escalation_time_report = new services.JiraSalesforceEscalationTimeReportService();
    this.jira_salesforce_files = new services.JiraSalesforceFilesService();
    this.jira_salesforce_resolved_tickets_trend = new services.JiraSalesforceResolvedTicketsTrendService();
    this.jira_salesforce_files_report = new services.JiraSalesforceFilesReportService();

    //jira zendesk
    this.jira_zendesk_escalation_time_report = new services.JiraZendeskEscalationTimeReportService();
    this.jira_zendesk_files = new services.JiraZendeskFilesService();
    this.jira_zendesk_resolved_tickets_trend = new services.JiraZendeskResolvedTicketsTrendService();
    this.jira_zendesk_files_report = new services.JiraZendeskFilesReportService();

    //scm
    this.scm_repos = new services.SCMReposReportService();
    this.scm_file_types = new services.SCMFileTypeReportService();
    this.scm_committers = new services.SCMCommittersReportService();
    this.scm_resolution_time_report = new services.SCMResolutionTimeReportService();
    this.scm_review_collaboration_report = new services.SCMReviewCollaborationReportService();

    // praetorian
    this.praetorian_issues_list = new services.PraetorianIssuesListService();
    this.praetorian_issues_values = new services.PraetorianIssuesValuesService();
    this.praetorian_issues_aggs = new services.PraetorianIssuesAggsService();

    //bullseye
    this.bullseye_coverage_report = new services.BullseyeBuildCoverageService();
    this.bullseye_files_report = new services.BullseyeBuildsFilesService();
    this.bulleseye_builds_report = new services.BullseyeBuildsService();
    this.bullseye_filter_values = new services.BullseyeBuildFilterValuesService();

    // ncc group
    this.ncc_group_issues_list = new services.NccGroupIssuesListService();
    this.ncc_group_issues_values = new services.NccGroupIssuesValuesService();
    this.ncc_group_issues_aggs = new services.NccGroupIssuesAggsService();

    // snyk
    this.snyk_issues_list = new services.SnykIssuesListService();
    this.snyk_issues_report = new services.SnykIssuesReportService();
    this.snyk_issues_values = new services.SnykIssuesFilterValueService();

    // Azure job duration
    this.azure_pipeline_list = new services.AzurePipelineListService();
    this.azure_pipeline_values = new services.AzurePipelineFilterValueService();
    this.azure_pipeline_job_duration = new services.AzurePipelineJobDurationReportService();
    this.azure_pipeline_runs = new services.AzurePipelineRunReportService();

    // Jira V2 service
    this.jiraV2_generate_token = new services.JiraCloudGenerateTokenService();
    this.jiravV2_verify_connection = new services.JiraCloudVerifyConnectionService();

    //dashboard widgets
    this.dashboard_widget = new services.WidgetsService();
    this.zendesk_fields = new services.ZendeskFieldsService();
    this.zendesk_custom_filter_values = new services.ZendeskCustomFieldsValuesService();

    //Ticket Categorization Schemes
    this.ticket_categorization_scheme = new services.TicketCategorizationSchemesService();

    // Story Point Report
    this.story_point_report = new services.StoryPointReportService();
    // jira sprint
    this.jira_sprint_report = new services.JiraSprintService();
    this.team_allocation_report = new services.TeamAllocationReportService();
    this.jira_sprint_filters = new services.JiraSprintFilterService();
    this.epic_priority_trend_report = new services.EpicPriorityReportService();
    this.sprint_distribution_report = new services.JiraSprintDistributionReportService();

    //Ticket Categorization Schemes
    this.ticket_categorization_scheme = new services.TicketCategorizationSchemesService();

    // OrganizationUnit Service
    this.organization_unit_management = new services.OrganizationUnitService();
    this.organization_unit_version_control = new services.OrganizationUnitVersionService();
    this.organization_unit_filter_values = new services.OrganizationUnitFilterValuesService();
    this.organization_unit_productivity_score = new services.OrganizationUnitProductivityScoreService();
    this.org_units_for_integration = new services.OrganizationUnitsForIntegrationService();
    this.pivots_list = new services.OrganizationUnitPivotListService();
    this.pivots = new services.OrganizationUnitPivotCreateUpdateService();
    this.org_dashboard_list = new services.OrganizationUnitDashboardListService();
    this.ous_dashboards = new services.OrganizationUnitDashboardAssociationService();
    // jenkins integration
    this.jenkins_integrations = new services.JenkinsInstanceListService();
    this.teams_list = new services.TeamsListService();

    // Velocity Configs
    this.velocity_configs = new services.VelocityConfigsService();

    // lead time widgets
    this.lead_time_report = new services.LeadTimeReportService();
    this.lead_time_values = new services.LeadTimeValueService();

    // triage filters
    this.triage_grid_view_filters = new services.TriageGridViewFiltersService();
    this.triage_filters = new services.TriageFiltersService();

    // report docs
    this.report_docs = new services.ReportsDocsService();

    // lead time widgets
    this.lead_time_report = new services.LeadTimeReportService();

    // scm pr labels
    this.scm_pr_labels = new services.ScmPrLabelsService();

    //ORG USERS
    this.org_users = new services.RestOrgUsersService();
    this.org_users_version = new services.RestOrgUsersVersionService();
    this.org_users_import = new services.RestOrgUsersImportService();
    this.org_users_schema = new services.RestOrgUsersSchemaService();
    this.org_users_filter = new services.RestOrgUsersFilterService();
    this.org_users_contributors_roles = new services.RestOrgUsersContributorsRolesService();

    // ingestion monitoring
    this.ingestion_integration_status = new services.IngestionIntegrationStatusService();
    this.ingestion_integration_logs = new services.IngestionIntegrationLogService();
    this.user_profile = new services.userProfileService();

    // Issue management report
    this.issue_management_workitem_values = new services.IssueManagementWorkItemValuesService();
    this.issue_management_custom_field_values = new services.IssueManagementCustomFieldValues();
    this.issue_management_tickets_report = new services.IssueManagementTicketReport();
    this.issue_management_stage_time_report = new services.IssueManagementStageTimeReport();
    this.issue_management_age_report = new services.IssueManagementAgeReport();
    this.issue_management_hygiene_report = new services.IssueManagementHygieneReport();
    this.issue_management_list = new services.IssueManagementWorkItemListService();
    this.issue_management_story_point_report = new services.IssueManagementStoryPointReport();
    this.issue_management_effort_report = new services.IssueManagementEffortReport();

    this.issue_management_response_time_report = new services.IssueManagementResponseTimeReport();
    this.issue_management_resolution_time_report = new services.IssueManagementResolutionTimeReport();
    this.issue_management_workItem_Fields_list = new services.IssueManagementWorkItemFieldListService();
    this.issue_management_sprint_report = new services.IssueManagementSprintReport();
    this.issue_management_sprint_filters = new services.IssueManagementSprintListService();
    this.issue_management_attributes_values = new services.IssueManagementAttributesValues();
    this.issue_management_hops_report = new services.IssueManagementHopsReportService();
    this.issue_management_first_assignee_report = new services.IssueManagementFirstAssigneeReportService();
    this.workitem_priorities = new services.WorkItemPrioritiesService();

    this.issue_management_bounce_report = new services.IssueManagementBounceReportService();
    // propel run rerun
    this.propels_run_rerun = new services.RestPropelReRun();

    // Coverity reports
    this.coverity_defects_list = new services.CoverityDefectsListService();
    this.coverity_defects_values = new services.CoverityDefectsValueService();
    this.coverity_defects_report = new services.CoverityDefectsReportService();

    //code vol vs deployment
    this.code_vol_vs_deployment = new services.CodeVolVsDeployemntService();
    this.code_vol_vs_deployment_drilldown = new services.CodeVolVsDeployemntDrilldownService();

    this.trellis_profile_services = new services.TrellisProfileServices();
    this.trellis_user_permission = new services.TrellisUserPermissionService();
    // dev productivity
    this.dev_productivity_reports = new services.RestDevProdReportsService();
    this.dev_productivity_report_drilldown = new services.RestDevProdDrillDownService();
    this.dev_productivity_report_orgs = new services.RestDevProdReportOrgsService();
    this.dev_productivity_report_orgs_users = new services.RestDevProdReportOrgsUsersService();
    this.dev_productivity_org_unit_score_report = new services.RestDevProdOrgUnitScoreReportService();
    this.dev_productivity_user_score_report = new services.RestDevProdUserScoreReportService();
    this.dev_productivity_relative_score = new services.RestDevProdRelativeScoreService();
    this.dev_productivity_user_pr_activity = new services.RestDevProdUserPRActivityService();
    this.dev_productivity_pr_activity_report = new services.RestDevProdPRActivityReportService();
    this.individual_raw_stats_report = new services.RestDevRawStats();
    // new trellis service
    this.dev_productivity_parent = new services.RestDevProdParentService();

    //tenant state
    this.tenant_state = new services.RestTenantStateService();

    // BA 2.0 Effort Investment
    this.effort_investment_tickets = new services.EffortInvestmentTicketsReportService();
    this.effort_investment_story_points = new services.EffortInvestmentStoryPointsReportService();
    this.effort_investment_time_spent = new services.EITicketTimeSpentService();
    this.active_effort_investment_tickets = new services.EIActiveTicketsReportService();
    this.active_effort_investment_story_points = new services.EIActiveStoryPointsReportService();
    this.scm_jira_commits_count_ba = new services.EISCMCommitsCountService();

    // Azure Effort Investment
    this.azure_effort_investment_tickets = new services.IssueManagementEITicketCountFTE();
    this.azure_effort_investment_story_point = new services.IssueManagementEIStoryPointFTE();
    this.azure_effort_investment_commit_count = new services.IssueManagementEICommitCountFTE();
    this.azure_effort_investment_time_spent = new services.IssueManagementEITicketTimeSpentFTE();
    this.active_azure_ei_ticket_count = new services.IssueManagementEIActiveTicketCountFTE();
    this.active_azure_ei_story_point = new services.IssueManagementEIActiveStoryPointFTE();
    this.azure_team_allocation = new services.IssueManagementEITeamAllocation();
    // issues filters
    this.issue_filters = new services.IssueFiltersService();

    this.jira_stage_bounce_report = new services.JiraStageBounceReportService();
    this.issue_management_stage_bounce_report = new services.AzureStageBounceReportService();

    // SCM Dora
    this.scm_dora_lead_time = new services.scmDoraLeadTimeService();
    this.scm_dora_deployment_frequency = new services.scmDoraDeploymentFrequencyService();
    this.scm_dora_time_to_recover = new services.scmDoraTimeToRecoverService();
    this.scm_dora_failure_rate = new services.SCMDoraFailureRateService();
    this.self_onboarding_repos = new services.SelfOnboardingReposService();
    this.self_onboarding_repos_search = new services.SelfOnboardingReposSearchService();
    this.lead_time_by_time_spent_in_stages_report = new services.LeadTimeByTimeSpentInStagesReportService();
    this.jira_release_table_report = new services.jiraReleaseTableReportService();
    this.jira_release_table_report_drilldown = new services.jiraReleaseTableReportDrilldownService();

    // workspace
    this.workspace = new services.WorkspaceService();
    this.workspace_categories = new services.WorkspaceCategoriesService();

    // dora
    this.dora_change_failure_rate = new services.RestDoraChangeFailureReportsService();
    this.dora_deployment_frequence = new services.RestDoraDeploymentFrequencyReportsService();
    this.dora_drill_down_report = new services.RestDoraDrilldownReportsService();
    this.dora_lead_time_for_change = new services.RestDoraLeadTimeForChangeReportsService();
    this.dora_lead_time_for_change_drilldown = new services.RestDoraLeadTimeForChangeReportDrillDownService();
    this.dora_scm_commits_drilldown = new services.RestDoraCommitsDrillDownService();
    this.lead_time_for_change = new services.RestDoraLeadTimeForChangeService();
    this.mean_time_to_restore = new services.RestDoraMeanTimeForChangeService();
    this.lead_time_for_change_drilldown = new services.RestDoraLeadTimeForChangeDrilldownService();
    this.mean_time_to_restore_drilldown = new services.RestDoraMeanTimeToRestoreDrilldownChangeService();

    this.cicd_job_params = new services.JenkinsJobsExecutionParametersValueService();

    this.testrails_fields = new services.TestrailsFieldsService();
    this.testrails_custom_filter_values = new services.TestrailsCustomFieldsValuesService();
  }
}
