import * as RestURI from "constants/restUri";
import { basicMappingType } from "dashboard/dashboard-types/common-types";

export type supportedFilterType = {
  uri: string;
  values: Array<string>;
  skipFetchCustomData?: boolean;
  moreFilters?: basicMappingType<any>;
};

export const jiraSupportedFilters: supportedFilterType = {
  uri: "jira_filter_values",
  values: [
    "status",
    "priority",
    "issue_type",
    "assignee",
    "project",
    "component",
    "label",
    "reporter",
    "fix_version",
    "version",
    "resolution",
    "status_category"
  ]
};
export const jiraSupportedStatusFilter: supportedFilterType = {
  uri: "jira_filter_values",
  values: ["status"]
};
export const EISupportedFilters: supportedFilterType = {
  uri: "jira_filter_values",
  values: [
    "status",
    "priority",
    "issue_type",
    "assignee",
    "project",
    "component",
    "label",
    "reporter",
    "fix_version",
    "version",
    "resolution"
  ]
};

export const jiraSprintGoalSupportedFilters: supportedFilterType = {
  uri: "jira_filter_values",
  values: ["sprint"],
  skipFetchCustomData: true
};

export const microsoftIssueSupportedFilters = {
  uri: RestURI.MICROSOFT_ISSUES_FILTER_VALUES,
  values: ["category", "priority", "model", "project", "tag"]
};

export const githubPRsSupportedFilters: supportedFilterType = {
  uri: "github_prs_filter_values",
  values: ["repo_id", "creator", "state", "source_branch", "target_branch", "assignee", "reviewer", "label", "project"]
};

export const githubCommitsSupportedFilters: supportedFilterType = {
  uri: "github_commits_filter_values",
  values: ["repo_id", "committer", "author", "project"]
};

export const BACommitSupportedFIlters: supportedFilterType = {
  uri: "github_commits_filter_values",
  values: ["repo_id", "committer", "author", "project"],
  skipFetchCustomData: true
};

export const githubIssuesSupportedFilters: supportedFilterType = {
  uri: "scm_issues_filter_values",
  values: ["creator", "label", "state", "repo_id", "assignee", "project"]
};

export const scmIssuesTimeAcrossStagesSupportedFilters: supportedFilterType = {
  uri: "scm_issues_time_across_stages_filter_values",
  values: ["column", "project", "label", "repo_id", "assignee"]
};

export const jenkinsJobConfigSupportedFilters: supportedFilterType = {
  uri: "jenkins_job_config_filter_values",
  values: ["cicd_user_id", "job_name"]
};

export const jenkinsGithubJobSupportedFilters: supportedFilterType = {
  uri: "jenkins_jobs_filter_values",
  values: ["cicd_user_id", "job_status", "job_name", "project_name", "instance_name", "job_normalized_full_name"]
};

export const jenkinsCicdJobCountSupportedFilters: supportedFilterType = {
  uri: "jenkins_jobs_filter_values",
  values: ["stage_name", "step_name"]
};

export const jenkinsPipelineJobSupportedFilters: supportedFilterType = {
  uri: "jenkins_pipelines_jobs_filter_values",
  values: ["cicd_user_id", "job_status", "job_name", "instance_name", "job_normalized_full_name"]
};

export const azurePiplineJobSupportedFilters: supportedFilterType = {
  uri: "azure_pipeline_values",
  values: ["result", "project", "pipeline"]
};

export const cicdSupportedFilters: supportedFilterType = {
  uri: "cicd_filter_values",
  values: ["cicd_user_id", "author", "job_status", "job_name"]
};

export const scmCicdSupportedFilters: supportedFilterType = {
  uri: "cicd_filter_values",
  values: [
    "cicd_user_id",
    "author",
    "job_status",
    "job_name",
    "repo",
    "project_name",
    "instance_name",
    "job_normalized_full_name"
  ]
};

export const junitSupportedFilters: supportedFilterType = {
  uri: "jobs_run_tests_filter_values",
  values: ["job_status", "job_name", "cicd_user_id", "test_status", "test_suite"]
};

export const jiraSalesforceSupportedFilters: supportedFilterType = {
  uri: "jira_salesforce_filter_values",
  values: [
    "salesforce_priority",
    "salesforce_type",
    "salesforce_status",
    "salesforce_contact",
    "jira_priority",
    "jira_status",
    "jira_assignee",
    "jira_reporter",
    "jira_issue_type",
    "jira_epic",
    "jira_project",
    "jira_component",
    "jira_label"
  ]
};

export const jiraZenDeskSupportedFilters: supportedFilterType = {
  uri: "jira_zendesk_filter_values",
  values: [
    "jira_status",
    "jira_priority",
    "jira_issue_type",
    "jira_assignee",
    "jira_project",
    "jira_component",
    "jira_label",
    "zendesk_brand",
    "zendesk_type",
    "zendesk_priority",
    "zendesk_status",
    "zendesk_organization",
    "zendesk_assignee",
    "zendesk_requester",
    "zendesk_submitter"
  ]
};

export const pagerdutyServicesSupportedFilters: supportedFilterType = {
  uri: "services_report_aggregate_filter_values",
  values: ["pd_service", "incident_priority", "incident_urgency", "alert_severity"]
};

export const pagerdutyFilters: supportedFilterType = {
  uri: "pagerduty_filter_values",
  values: ["pd_service", "incident_priority", "incident_urgency", "alert_severity", "user_id"]
};

export const pagerdutyIncidentSupportedFilters: supportedFilterType = {
  uri: "pagerduty_filter_values",
  values: ["pd_service", "incident_priority", "incident_urgency", "alert_severity"]
};

export const salesForceSupportedFilters: supportedFilterType = {
  uri: "salesforce_filter_values",
  values: ["status", "priority", "type", "contact", "account_name"]
};

export const sonarqubeSupportedFilters: supportedFilterType = {
  uri: "sonarqube_filter_values",
  values: ["type", "project", "status", "organization", "severity", "author"]
};

export const sonarqubemetricsSupportedFilters: supportedFilterType = {
  uri: "sonarqube_metrics_values",
  values: ["project"]
};

export const testrailsSupportedFilters: supportedFilterType = {
  uri: "testrails_tests_values",
  values: ["type", "project", "status", "milestone", "test_plan", "test_run", "priority"]
};

export const zendeskSupportedFilters: supportedFilterType = {
  uri: "zendesk_filter_values",
  values: ["brand", "type", "priority", "status", "organization", "assignee", "requester", "submitter"]
};

export const githubFilesSupportedFilters: supportedFilterType = {
  uri: "scm_files_filter_values",
  values: ["repo_id", "project"]
};

export const githubJiraFilesSupportedFilters: supportedFilterType[] = [
  { uri: "scm_files_filter_values", values: ["repo_id", "project"] },
  { uri: "jira_filter_values", values: ["issue_type"] }
];

export const praetorianIssuesSupportedFilters: supportedFilterType = {
  uri: "praetorian_issues_values",
  values: ["category", "priority", "report_grade", "report_security", "service", "project", "tag"]
};

export const bullseyeSupportedFilters: supportedFilterType = {
  uri: "bullseye_filter_values",
  values: ["project", "job_name", "job_normalized_full_name", "name"]
};

export const nccGroupReportSupportedFilters: supportedFilterType = {
  uri: "ncc_group_issues_values",
  values: ["project", "risk", "category", "component", "tag"]
};

export const snykSupportedFilters: supportedFilterType = {
  uri: "snyk_issues_values",
  values: ["type", "severity", "project"]
};

export const leadTimeJiraSupportedFilters: supportedFilterType = {
  uri: "lead_time_filter_values",
  values: [
    "jira_status",
    "jira_priority",
    "jira_issue_type",
    "jira_assignee",
    "jira_project",
    "jira_component",
    "jira_label",
    "jira_reporter",
    "jira_fix_version",
    "jira_version",
    "jira_resolution",
    "jira_status_category"
  ]
};

export const issueManagementSupportedFilters: supportedFilterType = {
  uri: "issue_management_workitem_values",
  values: [
    "workitem_project",
    "workitem_status",
    "workitem_priority",
    "workitem_type",
    "workitem_status_category",
    "workitem_parent_workitem_id",
    "workitem_epic",
    "workitem_assignee",
    "workitem_version",
    "workitem_fix_version",
    "workitem_reporter",
    "workitem_label"
  ]
};

export const issueManagementStatusSupportedFilters: supportedFilterType = {
  uri: "issue_management_workitem_values",
  values: ["workitem_status"]
};
export const issueManagementEffortInvestmentSupportedFilters: supportedFilterType = {
  uri: "issue_management_workitem_values",
  values: [
    "workitem_project",
    "workitem_priority",
    "workitem_type",
    "workitem_parent_workitem_id",
    "workitem_epic",
    "workitem_assignee",
    "workitem_ticket_category",
    "workitem_version",
    "workitem_fix_version",
    "workitem_reporter",
    "workitem_label"
  ]
};

export const azureLeadTimeSupportedFilters: supportedFilterType = {
  uri: "issue_management_workitem_values",
  values: [
    "workitem_project",
    "workitem_status",
    "workitem_priority",
    "workitem_type",
    "workitem_status_category",
    "workitem_parent_workitem_id",
    "workitem_epic",
    "workitem_assignee",
    "workitem_version",
    "workitem_fix_version",
    "workitem_reporter",
    "workitem_label"
  ]
};

export const leadTimeCicdSupportedFilters: supportedFilterType = {
  uri: "lead_time_filter_values",
  values: ["repo_id", "creator", "state", "source_branch", "target_branch", "assignee", "reviewer", "label"]
};

export const leadTimeSingleStatAzureSupportedFilters: supportedFilterType = {
  uri: "scm_issue_management_workitem_values",
  values: [...azureLeadTimeSupportedFilters.values, ...leadTimeCicdSupportedFilters.values]
};

export const leadTimeSupportedFilters: supportedFilterType = {
  uri: "lead_time_filter_values",
  values: [...leadTimeJiraSupportedFilters.values, ...leadTimeCicdSupportedFilters.values]
};

export const coverityIssueSupportedFilters: supportedFilterType = {
  uri: "coverity_defects_values",
  values: [
    "impact",
    "category",
    "kind",
    "checker_name",
    "component_name",
    "type",
    "domain",
    "first_detected_stream",
    "last_detected_stream",
    "file",
    "function"
  ]
};

export const CodeVolVsDeployemntSupportedFilters: supportedFilterType = {
  uri: "code_vol_vs_deployment_values",
  values: [
    "build_cicd_user_id",
    "build_job_status",
    "build_job_name",
    "build_project_name",
    "build_instance_name",
    "build_repo",
    "build_job_normalized_full_name",
    "deploy_cicd_user_id",
    "deploy_job_status",
    "deploy_job_name",
    "deploy_project_name",
    "deploy_instance_name",
    "deploy_job_normalized_full_name"
  ]
};

export const rawStatsSupportedFilters: supportedFilterType = {
  uri: "dev_productivity_report_drilldown",
  values: []
};

export const doraScmPRsSupportedFilters: supportedFilterType = {
  uri: "github_prs_filter_values",
  values: ["repo_id", "creator", "project"]
};

export const doraJenkinsGithubJobSupportedFilters: supportedFilterType = {
  uri: "jenkins_jobs_filter_values",
  values: ["cicd_user_id", "job_status", "instance_name"]
};
