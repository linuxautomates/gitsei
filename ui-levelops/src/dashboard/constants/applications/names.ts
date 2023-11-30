import { IntegrationTypes } from "constants/IntegrationTypes";
import { MISCELLENEOUS_REPORTS } from "dashboard/reports/miscellaneous/constant";
import { uniq } from "lodash";
import {
  jiraSprintCommitToDoneMetricsOptions,
  jiraSprintCreepMetricsOptions,
  jiraSprintCreepToDoneMetricsOptions,
  jiraSprintPercentageTrendMetricsOptions,
  jiraSprintTrendMetricsOptions
} from "../../graph-filters/components/Constants";

export const MICROSOFT_APPLICATION_NAME = "microsoft";
export const MICROSOFT_ISSUES_REPORT_NAME = "microsoft_vulnerability_report";
export const JIRA_TICKETS_REPORT_NAME = "tickets_report";
export const PAGERDUTY_INCIDENT_REPORT_TRENDS_NAME = "pagerduty_incident_report_trends";
export const JENKINS_APPLICATION_NAME = "jenkins";
export const JENKINS_GITHUB_APPLICATION_NAME = "jenkinsgithub";

export const JENKINS_APPLICATIONS = [JENKINS_APPLICATION_NAME, JENKINS_GITHUB_APPLICATION_NAME];

export enum JENKINS_REPORTS {
  JENKINS_COUNT_SINGLE_STAT_REPORT = "jobs_count_single_stat_report",
  JOBS_COMMITS_LEAD_SINGLE_STAT_REPORT = "jobs_commits_lead_single_stat_report",
  JOBS_DURATION_SINGLE_STAT_REPORT = "jobs_duration_single_stat_report",
  JOBS_CHANGE_VOLUMES_SINGLE_STAT_REPORT = "jobs_change_volumes_single_stat_report",
  JOBS_CHANGE_VOLUMES_BY_FILE_SINGLE_STAT_REPORT = "jobs_change_volumes_by_file_single_stat_report",
  CICD_PIPELINE_JOBS_DURATION_REPORT = "cicd_pipeline_jobs_duration_report",
  CICD_PIPELINE_JOBS_DURATION_TREND_REPORT = "cicd_pipeline_jobs_duration_trend_report",
  CICD_SCM_JOBS_COUNT_REPORT = "cicd_scm_jobs_count_report",
  CICD_PIPELINE_JOBS_COUNT_REPORT = "cicd_pipeline_jobs_count_report",
  CICD_PIPELINE_JOBS_COUNT_TREND_REPORT = "cicd_pipeline_jobs_count_trend_report",
  CICD_SCM_JOBS_DURATION_REPORT = "cicd_scm_jobs_duration_report",
  JOBS_COUNT_TRENDS_REPORT = "jobs_count_trends_report",
  JOBS_COMMIT_LEADS_TRENDS_REPORT = "jobs_commit_leads_trends_report",
  JOBS_DURATIONS_TRENDS_REPORT = "jobs_durations_trends_report",
  JOBS_CHANGE_VOLUMES_TRENDS_REPORT = "jobs_change_volumes_trends_report",
  CICD_JOBS_COUNT_REPORT = "cicd_jobs_count_report",
  SCM_CODING_DAYS_REPORT = "github_coding_days_report",
  SCM_CODING_DAYS_SINGLE_STAT = "github_coding_days_single_stat",
  SCM_PRS_RESPONSE_TIME_SINGLE_STAT = "github_prs_response_time_single_stat",
  SCM_PRS_RESPONSE_TIME_REPORT = "github_prs_response_time_report",
  SCM_PRS_SINGLE_STAT = "github_prs_single_stat",
  SCM_PRS_REPORT = "github_prs_report",
  JOB_CONFIG_CHANGE_COUNTS_STAT = "jenkins_job_config_change_counts_stat",
  JOB_CONFIG_CHANGE_COUNTS = "jenkins_job_config_change_counts",
  JOB_CONFIG_CHANGE_COUNTS_TREND = "jenkins_job_config_change_counts_trend",
  CODE_VOLUME_VS_DEPLOYMENT_REPORT = "code_volume_vs_deployment_report",
  JOB_RUNS_TEST_REPORT = "job_runs_test_report",
  JOB_RUNS_TEST_TREND_REPROT = "job_runs_test_trend_report",
  JOB_RUNS_TEST_DURATION_REPORT = "job_runs_test_duration_report",
  JOB_RUNS_TEST_DURATION_TREND_REPROT = "job_runs_test_duration_trend_report"
}

export const jenkinsTrendReports = [
  // Currently unsupported
  JENKINS_REPORTS.CICD_PIPELINE_JOBS_DURATION_TREND_REPORT,
  JENKINS_REPORTS.CICD_PIPELINE_JOBS_COUNT_TREND_REPORT,
  JENKINS_REPORTS.JOBS_COUNT_TRENDS_REPORT,

  // Supported Reports
  JENKINS_REPORTS.JOBS_COMMIT_LEADS_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_DURATIONS_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_TRENDS_REPORT
];

export enum JIRA_SPRINT_REPORTS {
  COMMIT_TO_DONE = "sprint_commit_to_done_stat",
  SPRINT_CREEP = "sprint_creep_stat",
  SPRINT_CREEP_DONE = "sprint_creep_done_stat",
  SPRINT_METRICS_SINGLE_STAT = "sprint_metrics_single_stat",
  SPRINT_METRICS_PERCENTAGE_TREND = "sprint_metrics_percentage_trend",
  SPRINT_METRICS_TREND = "sprint_metrics_trend",
  SPRINT_IMPACT = "sprint_impact_estimated_ticket_report"
}

export enum PAGERDUTY_REPORT {
  PAGERDUTY_HOTSPOT_REPORT = "pagerduty_hotspot_report",
  PAGERDUTY_RELEASE_INCIDENT_REPORT = "pagerduty_release_incidents",
  PAGERDUTY_ACK_TREND_REPORT = "pagerduty_ack_trend",
  PAGERDUTY_AFTER_HOURS_REPORT = "pagerduty_after_hours",
  PAGERDUTY_INCIDENT_REPORT_TRENDS = "pagerduty_incident_report_trends"
}
export enum JIRA_SPRINT_DISTRIBUTION_REPORTS {
  SPRINT_DISTRIBUTION_REPORT = "sprint_distribution_retrospective_report"
}
export enum COMBINED_JIRA_SPRINT_REPORT {
  SRPINT_SINGLE_STAT = "sprint_metrics_single_stat",
  SRPINT_PERCENTAGE_TREND = "sprint_metrics_percentage_trend",
  SPRINT_TREND = "sprint_metrics_trend",
  SPRINT_DISTRIBUTION_REPORT = "sprint_distribution_retrospective_report"
}

export const jiraSprintMetricOptions = {
  [JIRA_SPRINT_REPORTS.COMMIT_TO_DONE]: jiraSprintCommitToDoneMetricsOptions,
  [JIRA_SPRINT_REPORTS.SPRINT_CREEP]: jiraSprintCreepMetricsOptions,
  [JIRA_SPRINT_REPORTS.SPRINT_CREEP_DONE]: jiraSprintCreepToDoneMetricsOptions,
  [JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND]: jiraSprintPercentageTrendMetricsOptions,
  [JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND]: jiraSprintTrendMetricsOptions,
  [JIRA_SPRINT_REPORTS.SPRINT_IMPACT]: jiraSprintCommitToDoneMetricsOptions
};

export enum LEAD_TIME_REPORTS {
  LEAD_TIME_SINGLE_STAT_REPORT = "lead_time_single_stat",
  JIRA_LEAD_TIME_TREND_REPORT = "lead_time_trend_report",
  JIRA_LEAD_TIME_BY_STAGE_REPORT = "lead_time_by_stage_report",
  SCM_PR_LEAD_TIME_BY_STAGE_REPORT = "scm_pr_lead_time_by_stage_report",
  SCM_PR_LEAD_TIME_TREND_REPORT = "scm_pr_lead_time_trend_report",
  JIRA_LEAD_TIME_BY_TYPE_REPORT = "lead_time_by_type_report",
  LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT = "lead_time_by_time_spent_in_stages_report"
}

export enum SUPPORT_REPORTS {
  SALESFORCE_BOUNCE_REPORT = "salesforce_bounce_report",
  ZENDESK_BOUNCE_REPORT = "zendesk_bounce_report",
  SALESFORCE_C2F_TRENDS = "salesforce_c2f_trends",
  ZENDESK_C2F_TRENDS = "zendesk_c2f_trends",
  JIRA_SALESFORCE_REPORT = "jira_salesforce_report",
  JIRA_ZENDESK_REPORT = "jira_zendesk_report",
  SALESFORCE_HOPS_REPORT = "salesforce_hops_report",
  ZENDESK_HOPS_REPORT = "zendesk_hops_report",
  JIRA_SALESFORCE_FILES_REPORT = "jira_salesforce_files_report",
  JIRA_ZENDESK_FILES_REPORT = "jira_zendesk_files_report",
  SALESFORCE_HYGIENE_REPORT = "salesforce_hygiene_report",
  ZENDESK_HYGIENE_REPORT = "zendesk_hygiene_report",
  SALESFORCE_RESOLUTION_TIME_REPORT = "salesforce_resolution_time_report",
  ZENDESK_RESOLUTION_TIME_REPORT = "zendesk_resolution_time_report",
  JIRA_SALESFORCE_ESCALATION_TIME_REPORT = "jira_salesforce_escalation_time_report",
  JIRA_ZENDESK_ESCALATION_TIME_REPORT = "jira_zendesk_escalation_time_report",
  SALESFORCE_TICKETS_REPORT = "salesforce_tickets_report",
  ZENDESK_TICKETS_REPORT = "zendesk_tickets_report",
  SALESFORCE_TIME_ACROSS_STAGES = "salesforce_time_across_stages",
  ZENDESK_TIME_ACROSS_STAGES = "zendesk_time_across_stages",
  SALESFORCE_TOP_CUSTOMERS_REPORT = "salesforce_top_customers_report",
  ZENDESK_TOP_CUSTOMERS_REPORT = "zendesk_top_customers_report"
}
export enum SALESFORCE_REPORTS {
  SALESFORCE_TICKETS_REPORT_TRENDS = "salesforce_tickets_report_trends",
  SALESFORCE_BOUNCE_REPORT = "salesforce_bounce_report",
  SALESFORCE_TICKETS_REPORT = "salesforce_tickets_report",
  SALESFORCE_HYGIENE_REPORT = "salesforce_hygiene_report",
  SALESFORCE_HOPS_REPORT = "salesforce_hops_report",
  SALESFORCE_RESOLUTION_TIME_REPORT = "salesforce_resolution_time_report",
  SALESFORCE_TOP_CUSTOMERS_REPORT = "salesforce_top_customers_report"
}

export enum ZENDESK_REPORTS {
  ZENDESK_RESPONSE_TIME_REPORT = "zendesk_response_time_report",
  ZENDESK_REOPENS_REPORT = "zendesk_reopens_report",
  ZENDESK_REPLIES_REPORT = "zendesk_replies_report",
  ZENDESK_AGENT_WAIT_TIME_REPORT = "zendesk_agent_wait_time_report",
  ZENDESK_REQUESTER_WAIT_TIME_REPORT = "zendesk_requester_wait_time_report",
  ZEKDESK_BOUNCE_REPORT_TRENDS = "zendesk_bounce_report_trends",
  ZENDESK_HOPS_REPORT_TRENDS = "zendesk_hops_report_trends",
  ZENDESK_RESOLUTION_TIME_REPORT_TRENDS = "zendesk_resolution_time_report_trends",
  ZENDESK_TICKETS_REPORT_TRENDS = "zendesk_tickets_report_trends",
  ZENDESK_HYGIENE_REPORT_TRENDS = "zendesk_hygiene_report_trends",
  ZENDESK_REQUESTER_WAIT_TIME_REPORT_TRENDS = "zendesk_requester_wait_time_report_trends",
  ZENDESK_AGENT_WAIT_TIME_REPORT_TRENDS = "zendesk_agent_wait_time_report_trends",
  ZENDESK_REPLIES_REPORT_TRENDS = "zendesk_replies_report_trends",
  ZENDESK_REOPENS_REPORT_TRENDS = "zendesk_reopens_report_trends",
  ZENDESK_RESPONSE_TIME_TREND_REPORT = "zendesk_response_time_report_trends"
}

export const supportReports = Object.values(SUPPORT_REPORTS);

export enum LEAD_TIME_ISSUE_REPORT {
  ISSUE_LEAD_TIME_TREND_REPORT = "lead_time_trend_report",
  ISSUE_LEAD_Time_BY_STAGE_REPORT = "lead_time_by_stage_report",
  ISSUE_LEAD_TIME_BY_TYPE_REPORT = "lead_time_by_type_report",
  LEAD_TIME_SINGLE_STAT = "lead_time_single_stat"
}

export enum ISSUE_MANAGEMENT_REPORTS {
  TICKETS_REPORT = "azure_tickets_report",
  TICKET_ISSUE_SINGLE_STAT = "azure_tickets_counts_stat",
  TICKET_REPORT_TREND = "azure_tickets_report_trends",
  TIME_ACROSS_STAGES = "azure_time_across_stages",
  HYGIENE_REPORT = "azure_hygiene_report",
  HYGIENE_REPORT_TREND = "azure_hygiene_report_trends",
  BACKLOG_TREND_REPORT = "azure_backlog_trend_report",
  EFFORT_INVESTMENT_TREND_REPORT = "azure_effort_investment_trend_report",
  EFFORT_INVESTMENT_SINGLE_STAT_REPORT = "azure_effort_investment_single_stat",
  RESPONSE_TIME_REPORT = "azure_response_time_report",
  RESOLUTION_TIME_REPORT = "azure_resolution_time_report",
  SPRINT_IMPACT_ESTIMATED_TICKET_REPORT = "azure_sprint_impact_estimated_ticket_report",
  RESPONSE_TIME_SINGLE_STAT_REPORT = "azure_response_time_counts_stat",
  RESPONSE_TIME_TREND_REPORT = "azure_response_time_report_trends",
  SPRINT_METRICS_SINGLE_STAT = "azure_sprint_metrics_single_stat",
  SPRINT_METRICS_TREND = "azure_sprint_metrics_trend",
  SPRINT_METRICS_PERCENTAGE_TREND = "azure_sprint_metrics_percentage_trend",
  RESOLUTION_TIME_SINGLE_STAT_REPORT = "azure_resolution_time_counts_stat",
  RESOLUTION_TIME_TREND_REPORT = "azure_resolution_time_report_trends",
  BOUNCE_REPORT = "azure_bounce_report",
  BOUNCE_REPORT_TRENDS = "azure_bounce_report_trends",
  BOUNCE_COUNT_STAT = "azure_bounce_counts_stat",
  HOPS_REPORT = "azure_hops_report",
  HOPS_REPORT_TRENDS = "azure_hops_report_trends",
  HOPS_REPORT_SINGLE_STAT = "azure_hops_counts_stat",
  FIRST_ASSIGNEE_REPORT = "azure_first_assignee_report",
  STAGE_BOUNCE_REPORT = "azure_stage_bounce_report",
  STAGE_BOUNCE_SINGLE_STAT = "azure_stage_bounce_single_stat",
  EFFORT_INVESTMENT_ENGINEER_REPORT = "azure_effort_investment_engineer_report",
  EFFORT_INVESTMENT_ALIGNMENT_REPORT = "azure_effort_alignment_report",
  ISSUE_LEAD_TIME_TREND_REPORT = "azure_lead_time_trend_report",
  ISSUE_LEAD_TIME_BY_STAGE_REPORT = "azure_lead_time_by_stage_report",
  ISSUE_LEAD_TIME_BY_TYPE_REPORT = "azure_lead_time_by_type_report",
  LEAD_TIME_SINGLE_STAT = "azure_lead_time_single_stat",
  AZURE_ISSUES_PROGRESS_REPORT = "azure_issues_progress_report",
  AZURE_PROGRAM_PROGRESS_REPORT = "azure_program_progress_report"
}

export enum JIRA_MANAGEMENT_TICKET_REPORT {
  TICKETS_REPORT = "tickets_report",
  TICKET_ISSUE_SINGLE_STAT = "tickets_counts_stat",
  TICKET_REPORT_TREND = "tickets_report_trends",
  TICKET_COUNT_BY_FIRST_ASSIGNEE = "jira_tickets_count_by_first_assignee",
  TIME_ACROSS_STAGES = "jira_time_across_stages",
  HYGIENE_REPORT = "hygiene_report",
  HYGIENE_REPORT_TREND = "hygiene_report_trends",
  BACKLOG_TREND_REPORT = "jira_backlog_trend_report",
  EFFORT_INVESTMENT_TREND_REPORT = "effort_investment_trend_report",
  EFFORT_INVESTMENT_SINGLE_STAT_REPORT = "effort_investment_single_stat",
  EFFORT_INVESTMENT_ENGINEER_REPORT = "jira_effort_investment_engineer_report",
  EFFORT_INVESTMENT_ALIGNMENT_REPORT = "jira_effort_alignment_report",
  RESPONSE_TIME_REPORT = "response_time_report",
  RESOLUTION_TIME_REPORT = "resolution_time_report",
  SPRINT_IMPACT_ESTIMATED_TICKET_REPORT = "sprint_impact_estimated_ticket_report",
  RESPONSE_TIME_SINGLE_STAT_REPORT = "response_time_counts_stat",
  RESPONSE_TIME_TREND_REPORT = "response_time_report_trends",
  SPRINT_METRICS_SINGLE_STAT = "sprint_metrics_single_stat",
  SPRINT_METRICS_TREND = "sprint_metrics_trend",
  SPRINT_GOAL = "sprint_goal",
  SPRINT_METRICS_PERCENTAGE_TREND = "sprint_metrics_percentage_trend",
  RESOLUTION_TIME_SINGLE_STAT_REPORT = "resolution_time_counts_stat",
  RESOLUTION_TIME_TREND_REPORT = "resolution_time_report_trends",
  BOUNCE_REPORT = "bounce_report",
  BOUNCE_REPORT_TRENDS = "bounce_report_trends",
  BOUNCE_COUNT_STAT = "bounce_counts_stat",
  HOPS_REPORT = "hops_report",
  HOPS_REPORT_TRENDS = "hops_report_trends",
  HOPS_REPORT_SINGLE_STAT = "hops_counts_stat",
  FIRST_ASSIGNEE_REPORT = "first_assignee_report",
  STAGE_BOUNCE_REPORT = "stage_bounce_report",
  STAGE_BOUNCE_SINGLE_STAT = "stage_bounce_single_stat",
  ASSIGNEE_TIME_REPORT = "assignee_time_report",
  PROGRESS_SINGLE_REPORT = "progress_single_report",
  SPRINT_DISTRIBUTION_REPORT = "sprint_distribution_retrospective_report",
  LEAD_TIME_TREND_REPORT = "lead_time_trend_report",
  LEAD_TIME_BY_STAGE_REPORT = "lead_time_by_stage_report",
  LEAD_TIME_BY_TYPE_REPORT = "lead_time_by_type_report",
  ISSUE_LEAD_TIME_TREND_REPORT = "lead_time_trend_report",
  LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT = "lead_time_by_time_spent_in_stages_report",
  JIRA_RELEASE_TABLE_REPORT = "jira_release_table_report"
}

export enum AZURE_LEAD_TIME_ISSUE_REPORT {
  ISSUE_LEAD_TIME_TREND_REPORT = "azure_lead_time_trend_report",
  ISSUE_LEAD_Time_BY_STAGE_REPORT = "azure_lead_time_by_stage_report",
  ISSUE_LEAD_TIME_BY_TYPE_REPORT = "azure_lead_time_by_type_report",
  LEAD_TIME_SINGLE_STAT = "azure_lead_time_single_stat"
}

export enum AZURE_SPRINT_REPORTS {
  COMMIT_TO_DONE = "azure_sprint_commit_to_done_stat",
  SPRINT_CREEP = "azure_sprint_creep_stat",
  SPRINT_CREEP_DONE = "azure_sprint_creep_done_stat",
  SPRINT_METRICS_SINGLE_STAT = "azure_sprint_metrics_single_stat",
  SPRINT_METRICS_PERCENTAGE_TREND = "azure_sprint_metrics_percentage_trend",
  SPRINT_METRICS_TREND = "azure_sprint_metrics_trend",
  SPRINT_IMPACT = "azure_sprint_impact_estimated_ticket_report"
}

export const leadTimeIssueReports = Object.values(LEAD_TIME_ISSUE_REPORT);

export const issueManagementReports = Object.values(ISSUE_MANAGEMENT_REPORTS);

export const jiraManagementTicketReport = Object.values(JIRA_MANAGEMENT_TICKET_REPORT);

export const allSprintMetricsReport = Object.values(COMBINED_JIRA_SPRINT_REPORT);

export const reportsSupportedAzureTeamFilter = [
  ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT,
  ISSUE_MANAGEMENT_REPORTS.TICKET_REPORT_TREND,
  ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
  ISSUE_MANAGEMENT_REPORTS.TIME_ACROSS_STAGES,
  ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
  ISSUE_MANAGEMENT_REPORTS.RESPONSE_TIME_REPORT,
  ISSUE_MANAGEMENT_REPORTS.RESPONSE_TIME_SINGLE_STAT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.RESPONSE_TIME_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_SINGLE_STAT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS.SPRINT_IMPACT_ESTIMATED_TICKET_REPORT,
  ISSUE_MANAGEMENT_REPORTS.SPRINT_METRICS_TREND,
  ISSUE_MANAGEMENT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
  ISSUE_MANAGEMENT_REPORTS.SPRINT_METRICS_SINGLE_STAT,
  AZURE_LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT,
  AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_TIME_BY_TYPE_REPORT,
  ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT,
  ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT_TREND,
  ISSUE_MANAGEMENT_REPORTS.BACKLOG_TREND_REPORT
];

export const azureSprintReports = Object.values(AZURE_SPRINT_REPORTS);

export const azureLeadTimeIssueReports = Object.values(AZURE_LEAD_TIME_ISSUE_REPORT);

// Both jira and azure-devops lead time reports.
export const jiraAzureScmAllLeadTimeReports = [
  ...azureLeadTimeIssueReports,
  ...Object.values(LEAD_TIME_ISSUE_REPORT),
  LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_TREND_REPORT,
  LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_BY_STAGE_REPORT
];

/** Use this array for the reports which support jira but do not support azure */
export const JIRA_REPORTS_NOT_SUPPORTING_AZURE = [
  JIRA_MANAGEMENT_TICKET_REPORT.ASSIGNEE_TIME_REPORT,
  JIRA_MANAGEMENT_TICKET_REPORT.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT,
  JIRA_MANAGEMENT_TICKET_REPORT.TICKET_COUNT_BY_FIRST_ASSIGNEE,
  JIRA_MANAGEMENT_TICKET_REPORT.SPRINT_GOAL,
  JIRA_MANAGEMENT_TICKET_REPORT.SPRINT_DISTRIBUTION_REPORT
];

export const jiraAzureScmAllLeadTimeReportsApplication = ["azure_devops", "jira_velocity", "scm_velocity"];

export const fileTypeFilterReports = ["github_commits_report", "scm_file_types_report"];

export const ZENDESK_BOUNCE_REPORT = "zendesk_bounce_report";
export const ZENDESK_HOPS_REPORT = "zendesk_hops_report";
export const ZENDESK_RESPONSE_TIME_REPORT = "zendesk_response_time_report";
export const ZENDESK_RESOLUTION_TIME_REPORT = "zendesk_resolution_time_report";
export const ZENDESK_TICKETS_REPORT = "zendesk_tickets_report";
export const ZENDESK_REOPENS_REPORT = "zendesk_reopens_report";
export const ZENDESK_REPLIES_REPORT = "zendesk_replies_report";
export const ZENDESK_AGENT_WAIT_TIME_REPORT = "zendesk_agent_wait_time_report";
export const ZENDESK_REQUESTER_WAIT_TIME_REPORT = "zendesk_requester_wait_time_report";
export const ZENDESK_TOP_CUSTOMERS_REPORT = "zendesk_top_customers_report";

export const SCM_FILES_REPORT = "scm_files_report";

export const scmCicdStatReportTypes = [
  JENKINS_REPORTS.JOBS_COMMITS_LEAD_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_BY_FILE_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JENKINS_COUNT_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JOBS_DURATION_SINGLE_STAT_REPORT
];

export const scmCicdReportTypes = [
  JENKINS_REPORTS.JOBS_COMMIT_LEADS_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_TRENDS_REPORT,
  JENKINS_REPORTS.CICD_JOBS_COUNT_REPORT,
  JENKINS_REPORTS.CICD_SCM_JOBS_DURATION_REPORT,
  JENKINS_REPORTS.JOBS_DURATIONS_TRENDS_REPORT
  //JENKINS_REPORTS.CODE_VOLUME_VS_DEPLOYMENT_REPORT,
];

export const scmCicdReportsWithMetric = [
  JENKINS_REPORTS.JOBS_COMMIT_LEADS_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_TRENDS_REPORT
];

export const scmCicdAzureReports = [
  JENKINS_REPORTS.CICD_JOBS_COUNT_REPORT,
  JENKINS_REPORTS.JENKINS_COUNT_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.CICD_SCM_JOBS_DURATION_REPORT,
  JENKINS_REPORTS.JOBS_DURATIONS_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_DURATION_SINGLE_STAT_REPORT
];

export const leadTimeReports = [
  LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT,
  LEAD_TIME_REPORTS.JIRA_LEAD_TIME_TREND_REPORT,
  LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT,
  LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_BY_STAGE_REPORT,
  LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_TREND_REPORT,
  LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_TYPE_REPORT,
  LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT
];

export const JENKINS_AZURE_REPORTS = [
  "cicd_scm_jobs_count_report",
  "cicd_jobs_count_report",
  "jobs_count_trends_report",
  "jobs_count_single_stat_report",
  "jobs_duration_single_stat_report",
  "cicd_scm_jobs_duration_report",
  "jobs_durations_trends_report",
  "jobs_commits_lead_single_stat_report",
  "jobs_commit_leads_trends_report",
  "jobs_change_volumes_single_stat_report",
  "jobs_change_volumes_trends_report"
];

export const jenkinsEndTimeFilterReports = [
  JENKINS_REPORTS.CICD_PIPELINE_JOBS_DURATION_REPORT,
  JENKINS_REPORTS.CICD_PIPELINE_JOBS_DURATION_TREND_REPORT,
  JENKINS_REPORTS.CICD_SCM_JOBS_COUNT_REPORT,
  JENKINS_REPORTS.CICD_PIPELINE_JOBS_COUNT_REPORT,
  JENKINS_REPORTS.CICD_PIPELINE_JOBS_COUNT_TREND_REPORT,
  JENKINS_REPORTS.CICD_SCM_JOBS_DURATION_REPORT,
  JENKINS_REPORTS.JOBS_COUNT_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_COMMIT_LEADS_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_DURATIONS_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_TRENDS_REPORT,
  JENKINS_REPORTS.CICD_JOBS_COUNT_REPORT,
  JENKINS_REPORTS.JENKINS_COUNT_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JOBS_DURATION_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JOBS_COMMITS_LEAD_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_SINGLE_STAT_REPORT
];

export const JENKINS_AZURE_URIS = ["jobs_count_report", "pipelines_jobs_duration_report", "jobs_duration_report"]; // need modifications
export const PREVIEW_DISABLED = "preview_disabled";

export const TIME_FILTER_RANGE_CHOICE_MAPPER = "timeFilterRangeChoiceMapper";

export const SCM_ADDITIONAL_KEYS_APPLICATIONS = [
  "github_prs",
  "github_commits",
  "github_issues",
  "jira",
  "azure_devops"
];

export const JIRA_GRAPH_TYPE_REPORTS = {
  BOUNCE_REPORT: "bounce_report",
  HOPS_REPORT: "hops_report",
  RESPONSE_TIME_REPORT: "response_time_report",
  FIRST_ASSIGNEE_REPORT: "first_assignee_report",
  RESOLUTION_TIME_REPORT: "resolution_time_report",
  TICKETS_REPORTS: "tickets_report",
  HYGIENE_REPORT: "hygiene_report",
  JIRA_ZENDESK_REPORT: "jira_zendesk_report",
  JIRA_SALESFORCE_REPORT: "jira_salesforce_report",
  JIRA_TICKET_COUNT_BY_FIRST_ASSIGNEE: "jira_tickets_count_by_first_assignee",
  JIRA_TIME_ACROSS_STAGES: "jira_time_across_stages",
  JIRA_ASSIGNEE_REPORT: "assignee_time_report"
};

export const JIRA_MAX_XAXIS_SUPPORTED_REPORTS = [
  JIRA_GRAPH_TYPE_REPORTS.BOUNCE_REPORT,
  JIRA_GRAPH_TYPE_REPORTS.HOPS_REPORT,
  JIRA_GRAPH_TYPE_REPORTS.RESPONSE_TIME_REPORT,
  JIRA_GRAPH_TYPE_REPORTS.FIRST_ASSIGNEE_REPORT,
  JIRA_GRAPH_TYPE_REPORTS.RESOLUTION_TIME_REPORT,
  JIRA_GRAPH_TYPE_REPORTS.TICKETS_REPORTS,
  JIRA_GRAPH_TYPE_REPORTS.JIRA_TIME_ACROSS_STAGES
];

export const SCM_MAX_RECORDS_TREND_REPORTS = [
  "github_prs_report_trends",
  "github_prs_merge_trends",
  "github_prs_first_review_trends",
  "github_prs_first_review_to_merge_trends",
  "github_issues_report_trends",
  "github_issues_first_response_report_trends"
];

export const projectMappingApplications = [
  IntegrationTypes.JIRA,
  IntegrationTypes.JIRA_SALES_FORCE,
  IntegrationTypes.JIRAZENDESK,
  IntegrationTypes.GITHUB
];

export const projectMappingKeys = ["project", "jira_project"];

export const SPRINT = "sprint";
export const SPRINT_JIRA_ISSUE_KEYS = "sprint_jira_issue_keys";

// * report constant key for PURE FE BASED FILTERS like time filters
// * PURE FE BASED means the filter options are FE only
export const FE_BASED_FILTERS = "fe_based_filters";

// * report constant key for transforming x axis title
export const CHART_X_AXIS_TITLE_TRANSFORMER = "chart_x_axis_title_transformer";

// * report constant key if we want to truncate x axis title
export const CHART_X_AXIS_TRUNCATE_TITLE = "chart_x_axis_truncate_title";

export const ALLOW_CHART_TOOLTIP_LABEL_TRANSFORM = "allow_chart_tooltip_label_transform";

export const ALLOW_ZERO_LABELS = "allow_zero_labels";

export const CHART_TOOLTIP_RENDER_TRANSFORM = "chart_tooltip_render_transform";

// * report constant key for chart data transformers and constants
export const CHART_DATA_TRANSFORMERS = "CHART_DATA_TRANSFORMERS";

// * report constant key to get the status of stacks filter based on applied filters
export const STACKS_FILTER_STATUS = "getStacksStatus";

// * report constant key containing all the information messages we need for filters
export const INFO_MESSAGES = "infoMessages";

// * prefix for coverity defects report
export const COVERITY_DEFECT_PREFIX = "cov_defect_";

// * key for holding existing report transformer in report constant
export const PREV_REPORT_TRANSFORMER = "prev_report_transformer";
export const PREV_COMPOSITE_REPORT_TRANSFORMER = "prev_composite_report_transformer";

//Filter No longer supported But need to remove from the payload.
export const NO_LONGER_SUPPORTED_FILTER = "NO_LONGER_SUPPORTED_FILTER";

export const DEPRECATED_REPORT = "DEPRECATED_REPORT";

export const cicdScmSortFilterNonTrendReports: string[] = [
  JENKINS_REPORTS.CICD_JOBS_COUNT_REPORT,
  JENKINS_REPORTS.CICD_SCM_JOBS_DURATION_REPORT
];

export const cicdScmSortFilterTrendReports: string[] = [
  JENKINS_REPORTS.JOBS_DURATIONS_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_COUNT_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_COMMIT_LEADS_TRENDS_REPORT,
  JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_TRENDS_REPORT
];

export const BITBUCKET_APPLICATIONS = ["bitbucket", "bitbucket_server"];

export enum COVERITY_REPORTS {
  COVERITY_ISSUE_REPORT = "coverity_issues_report",
  COVERITY_ISSUE_TREND_REPORT = "coverity_issues_trend_report",
  COVERITY_ISSUE_STAT_REPORT = "coverity_issues_stat_report"
}

export enum PRAETORIAN_REPORTS {
  PRAETORIAN_ISSUES_REPORT = "praetorian_issues_report"
}
export enum MICROSOFT_REPORT {
  MICROSOFT_ISSUES_REPORT_NAME = "microsoft_vulnerability_report"
}

export const coverityReports = [
  COVERITY_REPORTS.COVERITY_ISSUE_REPORT,
  COVERITY_REPORTS.COVERITY_ISSUE_STAT_REPORT,
  COVERITY_REPORTS.COVERITY_ISSUE_TREND_REPORT
];

export enum NCC_GROUP_REPORTS {
  VULNERABILITY_REPORT = "ncc_group_vulnerability_report"
}

export enum SNYK_REPORTS {
  SNYK_VULNERABILITY_REPORT = "snyk_vulnerability_report"
}

export const scmEnhancedReports = [
  JENKINS_REPORTS.SCM_PRS_REPORT,
  JENKINS_REPORTS.SCM_PRS_SINGLE_STAT,
  JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT,
  JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_SINGLE_STAT,
  "github_commits_report",
  "github_commits_single_stat",
  "github_prs_report_trends"
];

export const ALL_SINGLE_STAT_REPORTS = [
  JENKINS_REPORTS.JENKINS_COUNT_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JOBS_COMMITS_LEAD_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JOBS_DURATION_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_BY_FILE_SINGLE_STAT_REPORT,
  JENKINS_REPORTS.SCM_CODING_DAYS_SINGLE_STAT,
  JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_SINGLE_STAT,
  JENKINS_REPORTS.SCM_PRS_SINGLE_STAT,
  JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT,
  LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_SINGLE_STAT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.RESPONSE_TIME_SINGLE_STAT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.SPRINT_METRICS_SINGLE_STAT,
  ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_SINGLE_STAT_REPORT,
  ISSUE_MANAGEMENT_REPORTS.HOPS_REPORT_SINGLE_STAT,
  JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT,
  JIRA_MANAGEMENT_TICKET_REPORT.EFFORT_INVESTMENT_SINGLE_STAT_REPORT,
  JIRA_MANAGEMENT_TICKET_REPORT.RESPONSE_TIME_SINGLE_STAT_REPORT,
  JIRA_MANAGEMENT_TICKET_REPORT.SPRINT_METRICS_SINGLE_STAT,
  JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_SINGLE_STAT_REPORT,
  JIRA_MANAGEMENT_TICKET_REPORT.HOPS_REPORT_SINGLE_STAT,
  AZURE_LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT,
  AZURE_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT
];

export const HYGIENE_TREND_REPORT = [
  JIRA_MANAGEMENT_TICKET_REPORT.HYGIENE_REPORT_TREND,
  ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT_TREND
];

export const HIDE_ISSUE_MANAGEMENT_SYSTEM_DROPDOWN = "hide_issue_management_system_dropdown";

export const COMPARE_X_AXIS_TIMESTAMP = "COMPARE_X_AXIS_TIMESTAMP";

export const IMPLICITY_INCLUDE_DRILLDOWN_FILTER = "IMPLICITY_INCLUDE_DRILLDOWN_FILTER";
export const REPORT_FILTERS_CONFIG = "report_filters_config";
export const MULTI_SERIES_REPORT_FILTERS_CONFIG = "multi_series_report_filter_config";
export const HIDE_CUSTOM_FIELDS = "hide_custom_fields";
export const GET_VELOCITY_CONFIG = "get_velocity_config";
export const SIMPLIFY_VALUE = "simplify_value";
export const DEPRECATED_NOT_ALLOWED = "deprecated_&_not_allowed";
export const DEPRECATED_MESSAGE = "DEPRECATED_MESSAGE";
export const HIDE_TOTAL_TOOLTIP = "hide_total_tooltip";

export enum PAGERDUTY_REPORT {
  RESPONSE_REPORTS = "pagerduty_response_reports"
}

export enum SCM_REPORTS {
  ISSUES_TIME_RESOLUTION = "scm_issues_time_resolution_report",
  SCM_REVIEW_COLLABORATION_REPORT = "review_collaboration_report",
  REPOS_REPORT = "scm_repos_report",
  FILE_TYPES_REPORT = "scm_file_types_report",
  COMMITTERS_REPORT = "scm_committers_report",
  CODING_DAYS_REPORT = "github_coding_days_report",
  CODING_DAYS_SINGLE_STAT = "github_coding_days_single_stat",
  PRS_SINGLE_STAT = "github_prs_single_stat",
  COMMITS_SINGLE_STAT = "github_commits_single_stat",
  PRS_MERGE_TRENDS = "github_prs_merge_trends",
  PRS_REPORT = "github_prs_report",
  PRS_FIRST_REVIEW_TRENDS = "github_prs_first_review_trends",
  PRS_FIRST_REVIEW_TO_MERGE_TRENDS = "github_prs_first_review_to_merge_trends",
  ISSUES_REPORT = "github_issues_report",
  ISSUES_REPORT_TRENDS = "github_issues_report_trends",
  ISSUES_COUNT_SINGLE_STAT = "github_issues_count_single_stat",
  ISSUES_FIRST_RESPONSE_REPORT = "github_issues_first_reponse_report",
  ISSUES_FIRST_RESPONSE_TREND_REPORT = "github_issues_first_response_report_trends",
  ISSUES_FIRST_RESPONSE_COUNT_SINGLE_STAT = "github_issues_first_response_count_single_stat",
  PRS_MERGE_SINGLE_STAT = "github_prs_merge_single_stat",
  PRS_FIRST_REVIEW_SINGLE_STAT = "github_prs_first_review_single_stat",
  PRS_FIRST_REVIEW_MERGE_SINGLE_STAT = "github_prs_first_review_to_merge_single_stat",
  ISSUES_TIME_RESOLUTION_REPORT = "scm_issues_time_resolution_report",
  PR_LEAD_TIME_TREND_REPORT = "scm_pr_lead_time_trend_report",
  PR_LEAD_TIME_BY_STAG_REPORT = "scm_pr_lead_time_by_stage_report",
  PRS_RESPONSE_TIME_REPORT = "github_prs_response_time_report",
  COMMITS_REPORT = "github_commits_report",
  REWORK_REPORT = "scm_rework_report",
  SCM_FILES_REPORT = "scm_files_report",
  ISSUES_FIRST_RESPONSE_REPORT_TRENDS = "github_issues_first_response_report_trends",
  SCM_JIRA_FILES_REPORT = "scm_jira_files_report",
  SCM_PRS_RESPONSE_TIME_SINGLE_STAT = "github_prs_response_time_single_stat",
  SCM_TIME_ACROSS_STAGES = "scm_issues_time_across_stages_report"
}

export enum TESTRAILS_REPORTS {
  TESTRAILS_TESTS_REPORT = "testrails_tests_report",
  TESTRAILS_TESTS_TRENDS_REPORT = "testrails_tests_trend_report",
  TESTRAILS_TESTS_ESTIMATE_REPORT = "testrails_tests_estimate_report",
  TESTRAILS_TESTS_ESTIMATE_TRENDS_REPORT = "testrails_tests_estimate_trend_report",
  TESTRAILS_TESTS_ESTIMATE_FORECAST_REPORT = "testrails_tests_estimate_forecast_report",
  TESTRAILS_TESTS_ESTIMATE_FORECAST_TRENDS_REPORT = "testrails_tests_estimate_forecast_trend_report"
}

export enum SONARQUBE_REPORTS {
  SONARQUBE_ISSUES_REPORT = "sonarqube_issues_report",
  SONARQUBE_EFFORT_REPORT = "sonarqube_effort_report",
  SONARQUBE_ISSUES_REPORT_TRENDS = "sonarqube_issues_report_trends",
  SONARQUBE_EFFORT_REPORT_TRENDS = "sonarqube_effort_report_trends",
  SONARQUBE_METRICS_REPORT = "sonarqube_metrics_report",
  SONARQUBE_METRICS_TREND_REPORT = "sonarqube_metrics_trend_report",
  SONARQUBE_CODE_COMPLEXITY_REPORT = "sonarqube_code_complexity_report",
  SONARQUBE_CODE_COMPLEXITY_TRENDS_REPORT = "sonarqube_code_complexity_trend_report",
  SONARQUBE_CODE_DUPLICATION_REPORT = "sonarqube_code_duplication_report",
  SONARQUBE_CODE_DUPLICATION_TREND_REPORT = "sonarqube_code_duplication_trend_report"
}

export enum BULLSEYE_REPORTS {
  BULLSEYE_FUNCTION_COVERAGE_REPORT = "bullseye_function_coverage_report",
  BULLSEYE_BRANCH_COVERAGE_REPORT = "bullseye_branch_coverage_report",
  BULLSEYE_DECISION_COVERAGE_REPORT = "bullseye_decision_coverage_report",
  BULLSEYE_CODE_COVERAGE_REPORT = "bullseye_code_coverage_report",
  BULLSEYE_FUNCTION_COVERAGE_TREND_REPORT = "bullseye_function_coverage_trend_report",
  BULLSEYE_BRANCH_COVERAGE_TREND_REPORT = "bullseye_branch_coverage_trend_report",
  BULLSEYE_DECISION_COVERAGE_TREND_REPORT = "bullseye_decision_coverage_trend_report",
  BULLSEYE_CODE_COVERAGE_TREND_REPORT = "bullseye_code_coverage_trend_report"
}

// key will be used by the report constant to transform the filters for the graphs API's
export const GET_GRAPH_FILTERS = "get_graph_filters";
export const LABEL_TO_TIMESTAMP = "LABEL_TO_TIMESTAMP";
export const GET_FILTERS = "get_filters";

// It is used to add interval field in drilldown payload.
// Previously interval field was sent under filter key and was ignored by BE
// oldPayload: {filter: {interval: "week"}} newPayload: {interval: "week", filter: {...}}
export const INCLUDE_INTERVAL_IN_PAYLOAD = "INCLUDE_INTERVAL_IN_PAYLOAD";

export enum DEV_PRODUCTIVITY_REPORTS {
  PRODUCTIVITY_SCORE_BY_ORG_UNIT = "dev_productivity_org_unit_score_report",
  DEV_PRODUCTIVITY_SCORE_REPORT = "dev_productivity_score_report",
  DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT = "dev_productivity_pr_activity_report",
  INDIVIDUAL_RAW_STATS = "individual_raw_stats_report",
  ORG_RAW_STATS = "org_raw_stats_report"
}

export enum DORA_REPORTS {
  DEPLOYMENT_FREQUENCY_REPORT = "deployment_frequency_report",
  CHANGE_FAILURE_RATE = "change_failure_rate",
  LEADTIME_CHANGES = "leadTime_changes",
  MEANTIME_RESTORE = "meanTime_restore"
}

export enum LEAD_MTTR_DORA_REPORTS {
  LEAD_TIME_FOR_CHANGE = "dora_lead_time_for_change",
  MEAN_TIME_TO_RESTORE = "dora_mean_time_to_restore"
}
export const LEAD_TIME_MTTR_REPORTS = Object.values(LEAD_MTTR_DORA_REPORTS);

export const doraReports = Object.values(DORA_REPORTS);
export const DEV_PROD_TABLE_REPORTS = ["dev_productivity_org_unit_score_report", "dev_productivity_score_report"];
export const RAW_STATS_REPORTS = [
  DEV_PRODUCTIVITY_REPORTS.INDIVIDUAL_RAW_STATS,
  DEV_PRODUCTIVITY_REPORTS.ORG_RAW_STATS
];

export const ForceFullWidth = [
  "salesforce_hygiene_report",
  "hygiene_report",
  "azure_hygiene_report",
  "zendesk_hygiene_report",
  "jira_burndown_report",
  "effort_investment_team_report",
  "lead_time_by_stage_report",
  "scm_pr_lead_time_by_stage_report",
  "lead_time_by_type_report",
  "dashboard_notes",
  "review_collaboration_report",
  "effort_investment_trend_report",
  "jira_effort_investment_engineer_report",
  "effort_investment_single_stat",
  "azure_effort_investment_single_stat",
  "review_collaboration_report",
  "jira_effort_alignment_report",
  JIRA_MANAGEMENT_TICKET_REPORT.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT,
  DEV_PRODUCTIVITY_REPORTS.PRODUCTIVITY_SCORE_BY_ORG_UNIT,
  DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_SCORE_REPORT,
  DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT,
  DEV_PRODUCTIVITY_REPORTS.INDIVIDUAL_RAW_STATS,
  DEV_PRODUCTIVITY_REPORTS.ORG_RAW_STATS,
  ISSUE_MANAGEMENT_REPORTS.AZURE_PROGRAM_PROGRESS_REPORT,
  DORA_REPORTS.CHANGE_FAILURE_RATE,
  DORA_REPORTS.DEPLOYMENT_FREQUENCY_REPORT,
  DORA_REPORTS.LEADTIME_CHANGES,
  DORA_REPORTS.MEANTIME_RESTORE,
  MISCELLENEOUS_REPORTS.TABLE_REPORTS,
  LEAD_MTTR_DORA_REPORTS.LEAD_TIME_FOR_CHANGE,
  LEAD_MTTR_DORA_REPORTS.MEAN_TIME_TO_RESTORE,
  JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT
];

export const HygieneReports = [
  "salesforce_hygiene_report",
  "hygiene_report",
  "azure_hygiene_report_trends",
  "azure_hygiene_report",
  "hygiene_report_trends",
  "zendesk_hygiene_report",
  "zendesk_hygiene_report_trends"
];

export const NOT_TRANSFORM_LABEL = ["github_prs_merge_single_stat", "github_prs_first_review_single_stat"];

export enum SCM_DORA_REPORTS {
  LEAD_TIME_FOR_CHNAGE = "lead_time_for_change",
  DEPLOYMENT_FREQUENCY = "deployment_frequency",
  TIME_TO_RECOVER = "scm_dora_time_to_recover",
  FAILURE_RATE = "scm_dora_failure_rate"
}

export const scmDoraReports = Object.values(SCM_DORA_REPORTS);

export const ALL_VELOCITY_PROFILE_REPORTS = [
  ...leadTimeReports,
  ...azureLeadTimeIssueReports,
  ...scmDoraReports,
  JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT
];

export const LEAD_TIME_BY_STAGE_REPORTS = [
  AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_Time_BY_STAGE_REPORT,
  LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT,
  LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_BY_STAGE_REPORT,
  LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT
];

export const WORKSPACES = "Projects";
export const INTEGRATION_WARNING = "INTEGRATION_WARNING";
export const WORKSPACE_NAME_MAPPING = {
  [WORKSPACES]: "Project",
  [INTEGRATION_WARNING]: "Delete integration(s) from Project?"
};

//key for chart legend dataKey
export const TRANSFORM_LEGEND_DATAKEY = "transform_legend_datakey";
export const TRANSFORM_LEGEND_LABEL = "TRANSFORM_LEGEND_LABEL";

export const LTFC_MTTR_REPORTS = [DORA_REPORTS.LEADTIME_CHANGES, DORA_REPORTS.MEANTIME_RESTORE];
export const azureIterationSupportableReports = uniq([
  ...issueManagementReports,
  ...Object.values(AZURE_LEAD_TIME_ISSUE_REPORT),
  ...Object.values(COMBINED_JIRA_SPRINT_REPORT),
  ...Object.values(AZURE_SPRINT_REPORTS),
  ...Object.values(LEAD_MTTR_DORA_REPORTS)
]);

export const GET_WIDGET_TITLE_INTERVAL = "GET_WIDGET_TITLE_INTERVAL";
