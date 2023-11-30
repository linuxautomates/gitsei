import {
  genericDrilldownTransformer,
  jiraDrilldownTransformer,
  levelopsDrilldownTransformer,
  praetorianDrilldownTransformer,
  nccGroupDrilldownTransformer,
  microsoftDrilldownTransformer,
  jenkinsDrilldownTransformer,
  topCustomersReportDrilldownTransformer,
  devRawStatsDrilldownTransformer
} from "dashboard/helpers/drilldown-transformers";
import {
  BullseyeTableConfig,
  GithubIssuesTableConfig,
  GithubPRSTableConfig,
  JenkinsGithubTableConfig,
  JenkinsJobCountTableConfig,
  JenkinsPipelineTableConfig,
  JiraTableConfig,
  JunitTestTableConfig,
  LevelOpsAssessmentTableConfig,
  LevelopsIssuesTableConfig,
  NccGroupTableConfig,
  PraetorianTableConfig,
  MicrosoftIssuesTableConfig,
  SalesForceTableConfig,
  sonarqubeMetricsTableConfig,
  sonarqubeTableConfig,
  TestrailsTableConfig,
  ZendeskTableConfig,
  SnykTableConfig,
  GithubCommitsTableConfig,
  JiraLeadTimeTableConfig,
  rawStatsTableConfig
} from "dashboard/pages/dashboard-tickets/configs";
import {
  bullseyeSupportedFilters,
  cicdSupportedFilters,
  githubIssuesSupportedFilters,
  githubPRsSupportedFilters,
  jenkinsGithubJobSupportedFilters,
  jenkinsJobConfigSupportedFilters,
  jenkinsPipelineJobSupportedFilters,
  jiraSupportedFilters,
  junitSupportedFilters,
  praetorianIssuesSupportedFilters,
  microsoftIssueSupportedFilters,
  salesForceSupportedFilters,
  sonarqubemetricsSupportedFilters,
  sonarqubeSupportedFilters,
  supportedFilterType,
  testrailsSupportedFilters,
  zendeskSupportedFilters,
  nccGroupReportSupportedFilters,
  snykSupportedFilters,
  githubCommitsSupportedFilters,
  scmCicdSupportedFilters,
  leadTimeJiraSupportedFilters,
  leadTimeCicdSupportedFilters,
  issueManagementSupportedFilters,
  azureLeadTimeSupportedFilters,
  coverityIssueSupportedFilters,
  CodeVolVsDeployemntSupportedFilters,
  issueManagementEffortInvestmentSupportedFilters,
  githubFilesSupportedFilters,
  githubJiraFilesSupportedFilters,
  rawStatsSupportedFilters
} from "./supported-filters.constant";
import * as RestURI from "constants/restUri";
import * as AppName from "./applications/names";
import {
  azureEffortInvestmentTrendReportTableConfig,
  AzureBacklogReportColumns,
  azureStatTableConfig,
  azureTableConfig,
  effortInvestmentTrendReportTableConfig,
  JiraBacklogReportColumns,
  azureUtilityDrilldownFilterColumn,
  AssigneeReportTableConfig,
  JiraResponseTimeTableConfig,
  azureResponseTimeTableConfig,
  JiraReleaseTableConfig
} from "dashboard/pages/dashboard-tickets/configs/jiraTableConfig";
import {
  azureBacklogTrendReportTransformer,
  azureDrilldownTransformer,
  effortInvestmentEngineerReportDrilldownTransformer,
  effortInvestmentTrendReportDrilldownTransformer,
  epicPriorityDrilldownTransformer,
  jiraBacklogDrillDownTransformer,
  jiraBounceReportDrillDownTransformer,
  jiraBurndownDrilldownTransformer,
  sprintMetricTrendDrilldownTransformer
} from "dashboard/helpers/drilldown-transformers/jiraDrilldownTransformer";
import { statDrilldownTransformer } from "dashboard/helpers/drilldown-transformers/jiraDrilldownTransformer";
import { JiraStatTableConfig } from "../pages/dashboard-tickets/configs/jiraTableConfig";
import {
  sprintMetricChurnRateColumns,
  sprintMetricStatTableConfig,
  sprintMetricStatUnitColumns
} from "dashboard/pages/dashboard-tickets/configs/sprintSingleStatTableConfig";
import {
  JenkinsGithubJobRunTableConfig,
  JenkinsPipelineJobTableConfig,
  ScmCicdTableConfig
} from "dashboard/pages/dashboard-tickets/configs/jenkinsTableConfig";
import {
  scmCommittersTableConfig,
  SCMFilesTableConfig,
  scmFileTypeConfig,
  SCMIssueTimeAcrossStagesTableConfig,
  SCMJiraFilesTableConfig,
  scmPRSFirstReviewTableConfig,
  scmReposTableConfig,
  SCMResolutionTableConfig,
  scmReviewCollaboration,
  topCustomerTableConfig
} from "dashboard/pages/dashboard-tickets/configs/githubTableConfig";
import {
  AzureLeadTimeTableConfig,
  LeadTimeByTimeSpentInStageTableConfig,
  SCMLeadTimeTableConfig
} from "../pages/dashboard-tickets/configs/leadTimeTableConfig";
import {
  azureSprintMetricTrendColumns,
  sprintMetricTrendColumns
} from "dashboard/pages/dashboard-tickets/configs/sprintMetricTrendReportTableConfig";
import { SprintNodeType } from "../../custom-hooks/helpers/constants";
import { MAX_DRILLDOWN_COLUMNS } from "./filter-key.mapping";
import {
  enhancedSCMDrilldownTransformer,
  githubFilesDrilldownTransformer,
  scmDrilldownTranformerForIncludesFilter,
  scmIssueResolutionTimeDrilldownTransformer,
  scmIssuesFirstReponseTrendDrilldownTransformer,
  scmIssueTimeAcrossStagesDrilldownTransformer,
  SCMPRSFirstReviewToMergeTrendsDrilldownTransformer,
  SCMPRSFirstReviewTrendsDrilldownTransformer,
  scmPrsMergeTrendsDrilldownTransformer,
  SCMReviewCollaborationDrilldownTransformer
} from "dashboard/helpers/drilldown-transformers/githubDrilldownTransformer";
import { CoverityDefectTableConfig } from "dashboard/pages/dashboard-tickets/configs/coverity-table-configs";
import { coverityDrillDownTransformer } from "dashboard/helpers/drilldown-transformers/coverityDrilldownTransformer";
import {
  DoraleadTimeMTTRDrilldownTransformer,
  leadTimeByTimeSpentInStagesDrilldownTransformer,
  leadTimeDrilldownTransformer
} from "dashboard/helpers/drilldown-transformers/leadTimeDrilldownTransformer";
import { jiraIssueTimeAcrossStagesDrilldownTransformer } from "dashboard/helpers/drilldown-transformers/jiraIssueTimeAcrosstagesDrilldownTransformer";
import { azureIssueTimeAcrossStagesDrilldownTransformer } from "dashboard/helpers/drilldown-transformers/azureIssueTimeAcrossStagesDrilldownTransformer";
import {
  ReportDrilldownColTransFuncType,
  ReportDrilldownFilterTransFuncType
} from "dashboard/dashboard-types/common-types";
import { eIDrilldownColumnTransformFunc } from "dashboard/helpers/drilldown-column-transformers/effortInvestmentTrendReportDrilldownColumns.transform";
import { levelopsTableReportDrilldownTransformer } from "dashboard/helpers/drilldown-transformers/levelopsTableReportDrilldownTransformer";
import { SalesForceTopCustomerTableConfig } from "dashboard/pages/dashboard-tickets/configs/salesforceTableConfig";
import { leadTimeByTimeSpentTransformDrilldownRecords } from "dashboard/reports/jira/lead-time-by-time-spent-in-stages/helper";
import { ColumnProps } from "antd/lib/table";
import { renderLeadTimeByTimeSpentInStageDynamicColumns } from "custom-hooks/helpers/leadTime.helper";
import {
  DeploymentFrequencyTableConfigIM,
  LeadTimeForChangeTableConfigIMAdo
} from "dashboard/pages/dashboard-tickets/configs/doraMatrixTableConfig";
import { doraDrillDownTransformer } from "dashboard/helpers/drilldown-transformers/doraDrillDownTransformer";
import {
  doraDrilldownColumnTransformer,
  doraLeadTimeForChangeDrilldownColumnTransformer,
  doraDrilldownSpecificSupportedFilters,
  leadTimeForChangeDrilldownColumnTransformer
} from "dashboard/helpers/drilldown-column-transformers/doraWorkflowProfileDrilldownColumn.transform";
import {
  dynamicColumnsConfigs,
  getExtraDrilldownProps,
  getSprintMetricSingleStatDrilldownProps,
  transformDrilldownRecords
} from "dashboard/reports/dora/Leadtime-changes/helper";
import { doraSupportedFilters } from "dashboard/reports/dora/constants";
import { getChangeFailureRateExtraDrilldownProps } from "dashboard/reports/dora/failure-rate/helper";
import { getDeploymentFrequencyExtraDrilldownProps } from "dashboard/reports/dora/deployment-frequency/helper";
import { getDoraExtraDrilldownProps } from "dashboard/reports/dora/DoraLeadTimeForChange/helper";
import { leadTimeByTimeSpentInStagesDynamicColTransform } from "dashboard/helpers/drilldown-column-transformers/leadTimeByTimeSpentInStagesDrilldownColumnTransform";
import {
  getCustomColumn,
  getTestrailsExtraDrilldownProps,
  testRailDrilldownColumnTransformer
} from "dashboard/reports/testRails/tests-report/filter.config";
import {
  cicdJobCountDrilldownColumnTransformer,
  getFilterConfigCicdJobCountDrillDown
} from "dashboard/reports/jenkins/cicd-jobs-count-report/filters.config";
import {
  cicdPipelineJobDrilldownColumnTransformer,
  getFilterConfigCicdPipelineJobDrillDown
} from "dashboard/reports/jenkins/cicd-pipeline-jobs-duration-report/filters.config";
import {
  cicdPipelineJobCountTrendsDrilldownColumnTransformer,
  getFilterConfigCicdPipelineJobCountTrendsDrillDown
} from "dashboard/reports/jenkins/cicd-pipeline-jobs-count-trend-report/filters.config";
import { JiraReleaseDrillDownTransformer } from "dashboard/reports/jira/jira-release-table-report/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export type DrillDownType = {
  title: string;
  uri: string;
  application: string;
  columns: any[];
  columnsWithInfo?: string[]; // dataIndex of the column
  supported_filters: supportedFilterType | supportedFilterType[];
  drilldownTransformFunction: (data: any) => void;
};

export type DrillDownExtraProps = {
  hasDynamicColumns?: boolean;
  renderDynamicColumns?: (key: string, activeKey?: string | undefined) => ColumnProps<any> | undefined;
  transformRecordsData?: (data: any) => any;
  scroll?: any;
  widgetId?: string;
  activeColumn?: string;
  customisedScroll?: boolean;
  shouldDerive?: string[];
};

export type DrillDownTypeWithDefaultSort = DrillDownType & { defaultSort: any[] };
export type DrillDownTypeWithDefaultSortFunc = DrillDownType & { defaultSort: (id: any) => void };
export type DrillDownWithColumnTranformerType = DrillDownType & {
  drilldownColumnTransformer?: ReportDrilldownColTransFuncType;
  getExtraDrilldownProps?: (params: any) => DrillDownExtraProps;
  defaultSort?: (id: any) => void;
  getCustomColumn?: (data: any) => void;
};
export type DrillDownTypeWithTransformRecords = DrillDownWithColumnTranformerType & {
  transformRecords: (data: any) => any;
};

export type DrilldownTypeWithDynamicColumnRenderer = DrillDownType & {
  renderDynamicColumns: (key: string, activeKey?: string | undefined) => ColumnProps<any> | undefined;
  drilldownDynamicColumnTransformer?: ReportDrilldownColTransFuncType;
};

export type DrilldownTypeWithSupportExpandRow = DrillDownType & {
  supportExpandRow: boolean;
};

/** For extending supported filters which are not required in widget configuration
 *  but required in drilldown filters
 */
export type DrillDownWithSpecificSupportedFilters = DrillDownWithColumnTranformerType & {
  drilldownSpecificSupportedFilters: supportedFilterType;
};

export type DoraDrillDownWithSpecificSupportedFilters = DrillDownWithColumnTranformerType & {
  doraDrilldownSpecificSupportedFilters: ReportDrilldownFilterTransFuncType;
  commitsUri?: string;
};

export type CicdDrillDownWithSpecificSupportedFilters = DrillDownWithColumnTranformerType & {
  cicdDrilldownSpecificSupportedFilters: ReportDrilldownFilterTransFuncType;
};

export type CustomDrillDownType = {
  title: string;
  application: string;
  uri?: string;
  columnsWithInfo?: string[]; // dataIndex of the column
  uriForNodeTypes?: any;
  columnsForNodeTypes?: any;
  supported_filters: supportedFilterType | supportedFilterType[];
  drilldownTransformFunction: (data: any) => void;
  [x: string]: any;
};

export type SupportDrillDownType = {
  application: string;
  uriForNodeTypes?: any;
  columnsForNodeTypes?: any;
  drilldownTransformFunction: (data: any) => void;
};

export const sprintMetricTrendReportDrilldown: CustomDrillDownType = {
  title: "Sprint Metrics Trend",
  application: IntegrationTypes.JIRA,
  uri: "jira_sprint_report",
  supported_filters: jiraSupportedFilters,
  [MAX_DRILLDOWN_COLUMNS]: 6,
  columnsForNodeTypes: {
    [SprintNodeType.SPRINT]: sprintMetricTrendColumns[SprintNodeType.SPRINT],
    [SprintNodeType.TIME_RANGE]: sprintMetricTrendColumns[SprintNodeType.TIME_RANGE]
  },
  drilldownTransformFunction: data => sprintMetricTrendDrilldownTransformer(data)
};

export const jiraAssigneeTimeDrilldown: CustomDrillDownType = {
  title: "Jira Assignee Time",
  uri: "assignee_time_report",
  application: IntegrationTypes.JIRA_ASSIGNEE_TIME_REPORT,
  columns: AssigneeReportTableConfig,
  columnsWithInfo: ["bounces", "hops", "response_time", "resolution_time"],
  supported_filters: jiraSupportedFilters,
  drilldownTransformFunction: data => jiraDrilldownTransformer(data)
};

export const levelOpsTableReportDrilldown: Omit<CustomDrillDownType, "supported_filters"> = {
  title: "Custom Table Report",
  application: IntegrationTypes.LEVELOPS_TABLE_REPORT,
  columns: [],
  drilldownTransformFunction: (data: any) => levelopsTableReportDrilldownTransformer(data)
};

export const levelOpsAssessmentDrillDown: Omit<DrillDownType, "supported_filters"> = {
  title: "Assessments",
  uri: "quiz",
  application: "levelops_assessment",
  columns: LevelOpsAssessmentTableConfig,
  drilldownTransformFunction: data => levelopsDrilldownTransformer(data)
};

export const levelOpsIssuesDrilldown: Omit<DrillDownType, "supported_filters"> = {
  title: "Issues",
  uri: "workitem",
  application: "levelops_issues",
  columns: LevelopsIssuesTableConfig,
  drilldownTransformFunction: data => levelopsDrilldownTransformer(data)
};

export const githubPRSDrilldown: DrillDownType = {
  title: "Github PRS Tickets",
  uri: "github_prs_tickets",
  application: "github_prs",
  columns: GithubPRSTableConfig,
  supported_filters: githubPRsSupportedFilters,
  drilldownTransformFunction: data => enhancedSCMDrilldownTransformer(data)
};

export const githubPRSMergeTrendDrilldown: DrillDownType = {
  title: "Github PRS Tickets",
  uri: "github_prs_tickets",
  application: "github_prs",
  columns: GithubPRSTableConfig,
  supported_filters: githubPRsSupportedFilters,
  drilldownTransformFunction: scmPrsMergeTrendsDrilldownTransformer
};

export const githubPRSFirstReviewTrendsDrilldown: DrillDownType = {
  title: "Github PRS First Review",
  uri: "github_prs_tickets",
  application: "github_prs_first_review",
  columns: scmPRSFirstReviewTableConfig,
  supported_filters: githubPRsSupportedFilters,
  drilldownTransformFunction: data => SCMPRSFirstReviewTrendsDrilldownTransformer(data)
};

export const githubPRSFirstReviewToMergeTrendsDrilldown: DrillDownType = {
  title: "Github PRS First Review",
  uri: "github_prs_tickets",
  application: "github_prs_first_review",
  columns: scmPRSFirstReviewTableConfig,
  supported_filters: githubPRsSupportedFilters,
  drilldownTransformFunction: data => SCMPRSFirstReviewToMergeTrendsDrilldownTransformer(data)
};

export const githubReviewCollaborationDrilldown: DrillDownType = {
  title: "Github PRS Tickets",
  uri: "github_prs_tickets",
  application: "scm_review_collaboration",
  columns: scmReviewCollaboration,
  supported_filters: githubPRsSupportedFilters,
  drilldownTransformFunction: data => SCMReviewCollaborationDrilldownTransformer(data)
};

export const githubPRSStatDrilldown: DrillDownType = {
  ...githubPRSDrilldown,
  application: "github_prs_stat",
  drilldownTransformFunction: data => statDrilldownTransformer(data)
};

export const githubCommitsStatDrilldown: DrillDownType = {
  title: "Github Commits Tickets",
  uri: "github_commits_tickets",
  application: "github_commits_stat",
  columns: GithubCommitsTableConfig,
  supported_filters: githubCommitsSupportedFilters,
  drilldownTransformFunction: data => statDrilldownTransformer(data)
};

export const githubIssuesDrilldown: DrillDownType = {
  title: "Github Issues",
  uri: "scm_issues_tickets",
  application: "github_issues",
  columns: GithubIssuesTableConfig,
  supported_filters: githubIssuesSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const githubIssuesFirstResponseReportTrendDrilldown: DrillDownType = {
  title: "Github Issues",
  uri: "scm_issues_tickets",
  application: "github_issues",
  columns: GithubIssuesTableConfig,
  supported_filters: githubIssuesSupportedFilters,
  drilldownTransformFunction: scmIssuesFirstReponseTrendDrilldownTransformer
};

export const scmResolutionDrillDown: DrillDownType = {
  title: "SCM Issues Resolution DrillDown",
  uri: "scm_issues_tickets",
  application: "github_issues_resolution",
  columns: SCMResolutionTableConfig,
  columnsWithInfo: ["scm_resolution_time"],
  supported_filters: githubIssuesSupportedFilters,
  drilldownTransformFunction: data => scmIssueResolutionTimeDrilldownTransformer(data)
};

export const scmIssueTimeAcrossStagesDrilldown: DrillDownType = {
  title: "SCM Issues Time Across Stages DrillDown",
  uri: "github_cards",
  application: "github_issues_resolution",
  columns: SCMIssueTimeAcrossStagesTableConfig,
  supported_filters: githubIssuesSupportedFilters,
  drilldownTransformFunction: data => scmIssueTimeAcrossStagesDrilldownTransformer(data)
};

export const githubIssuesStatDrilldown: DrillDownType = {
  ...githubIssuesDrilldown,
  application: "github_issues_stat",
  drilldownTransformFunction: data => statDrilldownTransformer(data)
};

export const jenkinsJobCountDrilldown: DrillDownType = {
  title: "Jenkins Job Config Change Count",
  uri: "jenkins_job_config_change_tickets",
  application: "jenkins_job_config",
  columns: JenkinsJobCountTableConfig,
  supported_filters: jenkinsJobConfigSupportedFilters,
  drilldownTransformFunction: data => jenkinsDrilldownTransformer(data)
};

export const jenkinsJobCountStatDrilldown: DrillDownType = {
  ...jenkinsJobCountDrilldown,
  application: "jenkins_job_config_stat",
  drilldownTransformFunction: data => statDrilldownTransformer(data)
};

export const junitTestDrilldown: DrillDownType = {
  title: "Junit Tests Report",
  uri: "jobs_run_tests",
  application: "job_runs_tests",
  columns: JunitTestTableConfig,
  supported_filters: junitSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const jenkinsGithubDrilldown: DrillDownType = {
  title: "Jenkins Github Tickets",
  uri: "cicd_scm",
  application: "jenkins_github",
  columns: JenkinsGithubTableConfig,
  supported_filters: cicdSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const jenkinsCodeVsDeploymentDrilldown: DrillDownType = {
  title: "Jenkins Github Tickets",
  uri: "code_vol_vs_deployment_drilldown",
  application: "jenkins_github",
  columns: ScmCicdTableConfig,
  columnsWithInfo: ["lines_modified", "files_modified"],
  supported_filters: scmCicdSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const jenkinsGithubStatDrilldown: DrillDownType = {
  ...jenkinsGithubDrilldown,
  application: "jenkins_github_stat",
  drilldownTransformFunction: data => statDrilldownTransformer(data)
};

export const jenkinsGithubJobRunDrilldown: CicdDrillDownWithSpecificSupportedFilters = {
  title: "Jenkins Github Job Runs",
  uri: "cicd_scm_job_runs_tickets",
  application: "jenkins_github_job_runs",
  columns: JenkinsGithubJobRunTableConfig,
  supported_filters: jenkinsGithubJobSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data),
  cicdDrilldownSpecificSupportedFilters: getFilterConfigCicdJobCountDrillDown,
  drilldownColumnTransformer: cicdJobCountDrilldownColumnTransformer
};

export const jenkinsGithubJobRunStatDrilldown: DrillDownType = {
  ...jenkinsGithubJobRunDrilldown,
  application: "jenkins_github_job_runs_stat",
  drilldownTransformFunction: data => statDrilldownTransformer(data)
};

export const jenkinsPipelineDrilldown: CicdDrillDownWithSpecificSupportedFilters = {
  title: "Jenkins Pipeline Jobs",
  uri: "pipeline_job_runs",
  application: "jenkins_pipeline_jobs",
  columns: JenkinsPipelineTableConfig,
  supported_filters: jenkinsPipelineJobSupportedFilters,
  drilldownTransformFunction: data => jenkinsDrilldownTransformer(data),
  cicdDrilldownSpecificSupportedFilters: getFilterConfigCicdPipelineJobCountTrendsDrillDown,
  drilldownColumnTransformer: cicdPipelineJobCountTrendsDrilldownColumnTransformer
};

export const jenkinsPipelineJobDrilldown: CicdDrillDownWithSpecificSupportedFilters = {
  title: "Jenkins Pipeline Jobs",
  uri: "cicd_scm_job_runs_tickets",
  application: "jenkins_pipeline_jobs",
  columns: JenkinsPipelineJobTableConfig,
  supported_filters: jenkinsPipelineJobSupportedFilters,
  drilldownTransformFunction: data => jenkinsDrilldownTransformer(data),
  cicdDrilldownSpecificSupportedFilters: getFilterConfigCicdPipelineJobDrillDown,
  drilldownColumnTransformer: cicdPipelineJobDrilldownColumnTransformer
};

export const azurePipelineJobDurationDrilldown: DrillDownType = {
  title: "Azure Pipeline Jobs",
  uri: "azure_pipeline_list",
  application: "azure_pipeline_jobs",
  columns: JenkinsPipelineTableConfig,
  supported_filters: [],
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const azurePipelineJobCountDrilldown: DrillDownType = {
  title: "Azure Pipeline Count",
  uri: "azure_pipeline_list",
  application: "azure_pipeline_count",
  columns: JenkinsGithubTableConfig,
  supported_filters: [],
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const jiraDrilldown: DrillDownTypeWithDefaultSort = {
  title: "Jira Tickets",
  uri: "jira_tickets",
  application: IntegrationTypes.JIRA,
  columns: JiraTableConfig,
  columnsWithInfo: ["bounces", "hops", "response_time", "resolution_time"],
  defaultSort: [{ id: "bounces", desc: true }],
  supported_filters: jiraSupportedFilters,
  drilldownTransformFunction: data => jiraDrilldownTransformer(data)
};

export const jiraResponseTimeTrendDrilldown: CustomDrillDownType = {
  ...jiraDrilldown,
  columns: JiraResponseTimeTableConfig,
  [MAX_DRILLDOWN_COLUMNS]: 4
};

export const jiraIssueTimeAcrossStagesDrilldown: DrillDownTypeWithDefaultSort = {
  ...jiraDrilldown,
  defaultSort: [{ id: "issue_created_at", desc: true }],
  drilldownTransformFunction: data => jiraIssueTimeAcrossStagesDrilldownTransformer(data)
};

export const jiraBounceReportDrilldown: DrillDownType & { defaultSort: any[] } = {
  title: "Jira Tickets",
  uri: "jira_tickets",
  application: IntegrationTypes.JIRA,
  columns: JiraTableConfig,
  columnsWithInfo: ["bounces", "hops", "response_time", "resolution_time"],
  defaultSort: [{ id: "bounces", desc: true }],
  supported_filters: jiraSupportedFilters,
  drilldownTransformFunction: data => jiraBounceReportDrillDownTransformer(data)
};

export const jiraResponseTimeReportDrilldown: CustomDrillDownType = {
  ...jiraDrilldown,
  columns: JiraResponseTimeTableConfig,
  [MAX_DRILLDOWN_COLUMNS]: 4
};

export const coverityDrilldown: DrillDownType & { defaultSort: any[] } = {
  title: "Coverity Defects",
  uri: "coverity_defects_list",
  application: "coverity",
  columns: CoverityDefectTableConfig,
  defaultSort: [],
  supported_filters: coverityIssueSupportedFilters,
  drilldownTransformFunction: data => coverityDrillDownTransformer(data)
};

export const azureDrilldown: DrillDownType & { defaultSort: any[] } = {
  title: "Azure Tickets",
  uri: "issue_management_list",
  application: IntegrationTypes.AZURE,
  columns: azureTableConfig,
  columnsWithInfo: ["bounces", "hops", "response_time", "resolution_time"],
  defaultSort: [],
  supported_filters: issueManagementSupportedFilters,
  drilldownTransformFunction: data => azureDrilldownTransformer(data)
};

export const azureResponseTimeDrilldown: CustomDrillDownType = {
  ...azureDrilldown,
  columns: azureResponseTimeTableConfig,
  [MAX_DRILLDOWN_COLUMNS]: 4
};

export const azureIssueTimeAcrossStagesDrilldown: DrillDownTypeWithDefaultSort = {
  ...azureDrilldown,
  drilldownTransformFunction: data => azureIssueTimeAcrossStagesDrilldownTransformer(data)
};

export const azureBacklogDrillDown: DrillDownType & { defaultSort: any[] } = {
  title: "Azure Tickets",
  uri: "issue_management_list",
  application: IntegrationTypes.AZURE,
  columns: AzureBacklogReportColumns,
  columnsWithInfo: ["bounces", "hops", "response_time", "resolution_time"],
  defaultSort: [],
  supported_filters: issueManagementSupportedFilters,
  drilldownTransformFunction: data => azureBacklogTrendReportTransformer(data)
};

// Jira Progress Report drilldown
export const jiraProgressReportDrilldown: DrillDownType & { defaultSort: any[] } = {
  ...jiraDrilldown,
  defaultSort: [{ id: "priority", desc: true }]
};

export const jiraBurndownReportDrilldown: DrillDownType & { defaultSort: any[] } = {
  ...jiraDrilldown,
  defaultSort: [{ id: "priority", desc: true }],
  drilldownTransformFunction: data => jiraBurndownDrilldownTransformer(data)
};

// Epic Priority Report
export const epicPriorityReportDrilldown: DrillDownType & { defaultSort: any[] } = {
  ...jiraProgressReportDrilldown,
  drilldownTransformFunction: data => epicPriorityDrilldownTransformer(data)
};

// Effort Investment Trend Report
export const effortInvestmentTrendReportDrilldown: DrillDownWithSpecificSupportedFilters = {
  title: "Jira Tickets",
  uri: "jira_tickets",
  application: IntegrationTypes.JIRA,
  columns: effortInvestmentTrendReportTableConfig,
  supported_filters: jiraSupportedFilters,
  drilldownTransformFunction: data => effortInvestmentTrendReportDrilldownTransformer(data),
  drilldownColumnTransformer: eIDrilldownColumnTransformFunc,
  drilldownSpecificSupportedFilters: {
    ...jiraSupportedFilters,
    values: [...jiraSupportedFilters.values, "epic", "ticket_category"]
  }
};

export const effortInvestmentEngineerReportDrilldown: DrillDownWithSpecificSupportedFilters = {
  title: "Jira Tickets",
  uri: "jira_tickets",
  application: IntegrationTypes.JIRA,
  columns: effortInvestmentTrendReportTableConfig,
  supported_filters: jiraSupportedFilters,
  drilldownTransformFunction: data => effortInvestmentEngineerReportDrilldownTransformer(data),
  drilldownColumnTransformer: eIDrilldownColumnTransformFunc,
  drilldownSpecificSupportedFilters: {
    ...jiraSupportedFilters,
    values: [...jiraSupportedFilters.values, "epic", "ticket_category"]
  }
};

// @ts-ignore
export const azureEffortInvestmentTrendReportDrilldown: DrillDownWithSpecificSupportedFilters = {
  ...azureDrilldown,
  columns: azureEffortInvestmentTrendReportTableConfig,
  drilldownTransformFunction: data => effortInvestmentTrendReportDrilldownTransformer(data),
  drilldownColumnTransformer: eIDrilldownColumnTransformFunc,
  drilldownSpecificSupportedFilters: issueManagementEffortInvestmentSupportedFilters
};

export const jiraStatDrilldown: DrillDownType & { defaultSort: any[] } = {
  ...jiraDrilldown,
  application: "jira_stat",
  columns: JiraStatTableConfig,
  drilldownTransformFunction: data => statDrilldownTransformer(data)
};

export const azureStatDrilldown: DrillDownType & { defaultSort: any[] } = {
  ...azureDrilldown,
  application: "azure_stat",
  columns: azureStatTableConfig,
  drilldownTransformFunction: data => statDrilldownTransformer(data)
};

export const jiraBacklogDrillDown: DrillDownType & { defaultSort: any[] } = {
  title: "Jira Tickets",
  uri: "jira_tickets",
  application: IntegrationTypes.JIRA,
  columns: JiraBacklogReportColumns,
  columnsWithInfo: ["bounces", "hops", "response_time", "resolution_time"],
  defaultSort: [{ id: "issue_created_at", desc: true }],
  supported_filters: jiraSupportedFilters,
  drilldownTransformFunction: data => jiraBacklogDrillDownTransformer(data)
};

export const microsoftDrilldown: DrillDownType = {
  title: "Microsoft Issues",
  uri: RestURI.MICROSOFT_ISSUES,
  application: AppName.MICROSOFT_APPLICATION_NAME,
  columns: MicrosoftIssuesTableConfig,
  supported_filters: microsoftIssueSupportedFilters,
  drilldownTransformFunction: data => microsoftDrilldownTransformer(data)
};

export const salesforceDrilldown: DrillDownType = {
  title: "Salesforce Tickets",
  uri: "salesforce_tickets",
  application: IntegrationTypes.SALESFORCE,
  columns: SalesForceTableConfig,
  supported_filters: salesForceSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const sonarQubeDrilldown: DrillDownType = {
  title: "SonarQube Tickets",
  uri: "sonarqube_tickets",
  application: "sonarqube",
  columns: sonarqubeTableConfig,
  supported_filters: sonarqubeSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const sonarQubeMetricDrilldown: DrillDownType = {
  title: "SonarQube Metrics Tickets",
  uri: "sonarqube_metrics_list",
  application: "sonarqube_metrics",
  columns: sonarqubeMetricsTableConfig,
  supported_filters: sonarqubemetricsSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const testRailsDrilldown: DrillDownWithColumnTranformerType = {
  title: "Testrail Tests",
  uri: "testrails_tests_list",
  application: IntegrationTypes.TESTRAILS,
  columns: TestrailsTableConfig,
  supported_filters: testrailsSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data),
  getExtraDrilldownProps: getTestrailsExtraDrilldownProps,
  getCustomColumn: getCustomColumn,
  drilldownColumnTransformer: testRailDrilldownColumnTransformer
};

export const zendeskDrilldown: DrillDownType = {
  title: "Zendesk Tickets",
  uri: "zendesk_tickets",
  application: IntegrationTypes.ZENDESK,
  columns: ZendeskTableConfig,
  supported_filters: zendeskSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const praetorianDrilldown: DrillDownType = {
  title: "Praetorian Issues",
  uri: "praetorian_issues_list",
  application: "praetorian",
  columns: PraetorianTableConfig,
  supported_filters: praetorianIssuesSupportedFilters,
  drilldownTransformFunction: data => praetorianDrilldownTransformer(data)
};

export const bullseyeDrilldown: DrillDownType = {
  title: "Bullseye",
  uri: "bullseye_files_report",
  application: "bullseye",
  columns: BullseyeTableConfig,
  supported_filters: bullseyeSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const nccGroupDrilldown: DrillDownType = {
  title: "NCC Group Issues",
  uri: "ncc_group_issues_list",
  application: "nccgroup",
  columns: NccGroupTableConfig,
  supported_filters: nccGroupReportSupportedFilters,
  drilldownTransformFunction: data => nccGroupDrilldownTransformer(data)
};

export const snykDrilldown: DrillDownType = {
  title: "Snyk Issues",
  uri: "snyk_issues_list",
  application: "snyk",
  columns: SnykTableConfig,
  supported_filters: snykSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const scmCicdDrilldown: DrillDownType = {
  ...jenkinsGithubDrilldown,
  columns: ScmCicdTableConfig,
  columnsWithInfo: ["lines_modified", "files_modified"],
  supported_filters: scmCicdSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const scmCicdStatDrilldown: DrillDownType = {
  ...scmCicdDrilldown,
  application: "jenkins_github_stat",
  drilldownTransformFunction: data => statDrilldownTransformer(data)
};

export const sprintMetricSingleStatDrilldown: DrillDownWithColumnTranformerType = {
  title: "Sprint Metrics",
  uri: "jira_sprint_report",
  application: "sprint_metrics_single_stat",
  columns: [...sprintMetricStatTableConfig, ...sprintMetricStatUnitColumns, ...sprintMetricChurnRateColumns],
  supported_filters: jiraSupportedFilters,
  getExtraDrilldownProps: getSprintMetricSingleStatDrilldownProps,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const azureSprintMetricSingleStatDrilldown: DrillDownType = {
  title: "Sprint Metrics",
  uri: "issue_management_sprint_report",
  application: "sprint_metrics_single_stat",
  columns: [...sprintMetricStatTableConfig, ...sprintMetricStatUnitColumns, ...azureUtilityDrilldownFilterColumn],
  supported_filters: issueManagementSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data)
};

export const azureSprintMetricTrendReportDrilldown: CustomDrillDownType = {
  title: "Sprint Metrics Trend",
  application: IntegrationTypes.AZURE,
  uri: "issue_management_sprint_report",
  supported_filters: issueManagementSupportedFilters,
  [MAX_DRILLDOWN_COLUMNS]: 6,
  columnsForNodeTypes: {
    [SprintNodeType.SPRINT]: azureSprintMetricTrendColumns[SprintNodeType.SPRINT],
    [SprintNodeType.TIME_RANGE]: azureSprintMetricTrendColumns[SprintNodeType.TIME_RANGE]
  },
  drilldownTransformFunction: data => sprintMetricTrendDrilldownTransformer(data)
};

export const jiraLeadTimeDrilldown: DrillDownTypeWithDefaultSortFunc = {
  title: "Jira Tickets",
  uri: "lead_time_values",
  application: "jira_velocity",
  columns: JiraLeadTimeTableConfig,
  supported_filters: leadTimeJiraSupportedFilters,
  drilldownTransformFunction: data => leadTimeDrilldownTransformer(data),
  defaultSort: id => [{ id, desc: true }]
};

export const leadTimeByTimeSpentInStagesDrilldown: DrillDownTypeWithTransformRecords &
  DrilldownTypeWithDynamicColumnRenderer &
  DrilldownTypeWithSupportExpandRow = {
  title: "Lead Time Spent",
  uri: "jira_tickets",
  application: IntegrationTypes.JIRA,
  columns: LeadTimeByTimeSpentInStageTableConfig,
  supported_filters: leadTimeJiraSupportedFilters,
  supportExpandRow: false,
  renderDynamicColumns: renderLeadTimeByTimeSpentInStageDynamicColumns,
  drilldownDynamicColumnTransformer: leadTimeByTimeSpentInStagesDynamicColTransform,
  transformRecords: leadTimeByTimeSpentTransformDrilldownRecords,
  drilldownTransformFunction: leadTimeByTimeSpentInStagesDrilldownTransformer
};

export const azureLeadTimeDrilldown: DrillDownTypeWithDefaultSortFunc = {
  title: "Azure Tickets",
  uri: "lead_time_values",
  application: IntegrationTypes.AZURE,
  columns: AzureLeadTimeTableConfig,
  supported_filters: azureLeadTimeSupportedFilters,
  drilldownTransformFunction: data => leadTimeDrilldownTransformer(data),
  defaultSort: id => [{ id, desc: true }]
};

export const scmLeadTimeDrilldown: DrillDownTypeWithDefaultSortFunc = {
  title: "Github PRS Tickets",
  uri: "lead_time_values",
  application: "scm_velocity",
  columns: SCMLeadTimeTableConfig,
  supported_filters: leadTimeCicdSupportedFilters,
  drilldownTransformFunction: data => genericDrilldownTransformer(data),
  defaultSort: id => [{ id, desc: true }]
};

export const salesforceTopCustomerDrilldown: DrillDownType = {
  title: "Salesforce Top Customer Report",
  uri: "salesforce_hygiene_report",
  columns: SalesForceTopCustomerTableConfig,
  supported_filters: salesForceSupportedFilters,
  application: "top_customer",
  drilldownTransformFunction: topCustomersReportDrilldownTransformer
};

export const zendeskTopCustomerReportDrilldown: DrillDownType = {
  title: "Zendesk Top Customer Report",
  uri: "zendesk_tickets_report",
  columns: topCustomerTableConfig,
  supported_filters: zendeskSupportedFilters,
  application: "top_customer",
  drilldownTransformFunction: topCustomersReportDrilldownTransformer
};
export const scmCommitsReportDrilldown: DrillDownType = {
  title: "Github Commits Tickets",
  uri: "github_commits_tickets",
  application: "github_commits",
  columns: GithubCommitsTableConfig,
  supported_filters: {
    ...githubCommitsSupportedFilters,
    values: [...githubCommitsSupportedFilters.values, "file_type"]
  },
  drilldownTransformFunction: scmDrilldownTranformerForIncludesFilter
};

export const scmFilesReportDrilldown: DrillDownType = {
  title: "Github Files",
  uri: "scm_files_report",
  application: "github_files",
  columns: SCMFilesTableConfig,
  supported_filters: githubFilesSupportedFilters,
  drilldownTransformFunction: githubFilesDrilldownTransformer
};

export const scmJiraFilesReportDrilldown: DrillDownType = {
  title: "Github/Jira Files",
  uri: "scm_jira_files_report",
  application: "github_jira_files",
  columns: SCMJiraFilesTableConfig,
  supported_filters: githubJiraFilesSupportedFilters,
  drilldownTransformFunction: githubFilesDrilldownTransformer
};

export const scmReposReportDrilldown: DrillDownType = {
  title: "Github or Bitbucket Repos",
  uri: "scm_repos",
  application: "scm_repos",
  columns: scmReposTableConfig,
  supported_filters: githubCommitsSupportedFilters,
  drilldownTransformFunction: genericDrilldownTransformer
};

export const scmCommittersReportDrilldown: DrillDownType = {
  title: "Github or Bitbucket Committers",
  uri: "scm_committers",
  application: "scm_committers",
  columns: scmCommittersTableConfig,
  supported_filters: githubCommitsSupportedFilters,
  drilldownTransformFunction: genericDrilldownTransformer
};

export const scmFilesTypesReportDrilldown: DrillDownType = {
  title: "Github or Bitbucket File Types",
  uri: "scm_file_types",
  application: "scm_file_types",
  columns: scmFileTypeConfig,
  supported_filters: {
    ...githubCommitsSupportedFilters,
    values: [...githubCommitsSupportedFilters.values, "file_type"]
  },
  drilldownTransformFunction: genericDrilldownTransformer
};

export const scmCodingDaysDrilldown: DrillDownType = {
  title: "Github Commits Tickets",
  uri: "github_commits_tickets",
  application: "github_commits",
  columns: GithubCommitsTableConfig,
  supported_filters: {
    ...githubCommitsSupportedFilters,
    values: [...githubCommitsSupportedFilters.values, "file_type"]
  },
  drilldownTransformFunction: scmDrilldownTranformerForIncludesFilter
};

export const scmReworkReportDrilldown: DrillDownType = {
  title: "Github Commits Tickets",
  uri: "github_commits_tickets",
  application: "github_commits",
  columns: GithubCommitsTableConfig,
  supported_filters: {
    ...githubCommitsSupportedFilters,
    values: [...githubCommitsSupportedFilters.values, "file_type"]
  },
  drilldownTransformFunction: scmDrilldownTranformerForIncludesFilter
};

export const rawStatsDrilldown: DrillDownType = {
  title: "Dev Raw Stats",
  uri: "dev_productivity_report_drilldown",
  application: "dev_raw_stats",
  columns: rawStatsTableConfig,
  supported_filters: rawStatsSupportedFilters,
  drilldownTransformFunction: data => devRawStatsDrilldownTransformer(data)
};

export const deploymentFrequencyDrilldown: DoraDrillDownWithSpecificSupportedFilters = {
  title: "Deployment Frequency Drilldown",
  uri: "dora_drill_down_report",
  application: "deployment_frequency_report",
  columns: DeploymentFrequencyTableConfigIM,
  supported_filters: doraSupportedFilters,
  drilldownTransformFunction: data => doraDrillDownTransformer(data),
  drilldownColumnTransformer: doraDrilldownColumnTransformer,
  doraDrilldownSpecificSupportedFilters: doraDrilldownSpecificSupportedFilters,
  getExtraDrilldownProps: getDeploymentFrequencyExtraDrilldownProps
};

export const changeFailureRateDrilldown: DoraDrillDownWithSpecificSupportedFilters = {
  title: "Change Failure Rate Drilldown",
  uri: "dora_drill_down_report",
  application: "change_failure_rate",
  columns: DeploymentFrequencyTableConfigIM,
  supported_filters: doraSupportedFilters,
  drilldownTransformFunction: data => doraDrillDownTransformer(data),
  drilldownColumnTransformer: doraDrilldownColumnTransformer,
  doraDrilldownSpecificSupportedFilters: doraDrilldownSpecificSupportedFilters,
  getExtraDrilldownProps: getChangeFailureRateExtraDrilldownProps
};

export const leadTimeForChangeDrilldown: DrillDownWithColumnTranformerType = {
  title: "Lead Time For Change Drilldown",
  uri: "dora_lead_time_for_change_drilldown",
  application: "leadTime_changes",
  columns: LeadTimeForChangeTableConfigIMAdo,
  supported_filters: [],
  drilldownTransformFunction: doraDrillDownTransformer,
  drilldownColumnTransformer: doraLeadTimeForChangeDrilldownColumnTransformer,
  getExtraDrilldownProps: getExtraDrilldownProps
};

export const DoraLeadTimeForChangeDrilldown: DrillDownWithColumnTranformerType = {
  title: "Dora Lead Time For Change",
  uri: "lead_time_for_change_drilldown",
  application: "dora_lead_mttr",
  columns: JiraLeadTimeTableConfig,
  supported_filters: leadTimeJiraSupportedFilters,
  drilldownTransformFunction: DoraleadTimeMTTRDrilldownTransformer,
  drilldownColumnTransformer: leadTimeForChangeDrilldownColumnTransformer,
  defaultSort: id => [{ id, desc: true }],
  getExtraDrilldownProps: getDoraExtraDrilldownProps
};

export const jiraReleaseTableDrilldown: DrillDownType = {
  title: "Jira Releases Report",
  uri: "jira_release_table_report_drilldown",
  application: IntegrationTypes.JIRA,
  columns: JiraReleaseTableConfig,
  columnsWithInfo: ["velocity_stage_total_time"],
  supported_filters: jiraSupportedFilters,
  drilldownTransformFunction: data => JiraReleaseDrillDownTransformer(data)
};
