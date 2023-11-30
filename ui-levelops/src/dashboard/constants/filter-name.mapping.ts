import { Dict } from "types/dict";

export const FILTER_NAME_MAPPING = "filterOptionMap";
export const VALUE_SORT_KEY = "VALUE_SORT_KEY";
export const ALLOWED_WIDGET_DATA_SORTING = "ALLOWED_WIDGET_DATA_SORTING";
export const WIDGET_VALIDATION_FUNCTION = "widget_validation_function";
export const WIDGET_DATA_SORT_FILTER_KEY = "sort_xaxis";
export const FILTER_PARENT_AND_VALUE_KEY = "FILTER_PARENT_AND_VALUE_KEY";
export const GET_PARENT_AND_TYPE_KEY = "GET_PARENT_AND_TYPE_KEY";
export const WIDGET_FILTER_PREVIEW_COUNT = "WIDGET_FILTER_PREVIEW_COUNT";
export const METADATA_FILTERS_PREVIEW = "METADATA_FILTERS_PREVIEW";
export const SUPPORTED_FILTERS_WITH_INFO = "SUPPORTED_FILTERS_WITH_INFO";
export const GET_WIDGET_CHART_PROPS = "get_widget_chart_props";
export const WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE = "WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE";

const scmPRbrnachFilterMapping = {
  source_branch: "Source Branch",
  target_branch: "Destination Branch"
};
export const jiraCommonFilterOptionsMapping: Dict<string, string> = {
  version: "Affects Version",
  ...scmPRbrnachFilterMapping
};

export const issueManagementCommonFilterOptionsMapping: Dict<string, string> = {
  version: "Affects Version",
  workitem_parent_workitem_id: "Parent Workitem id"
};

export const issueLeadTimeFilterOptionsMapping: Dict<string, string> = {
  version: "Affects Version",
  jira_status: "status",
  jira_priority: "priority",
  jira_issue_type: "issue type",
  jira_assignee: "assignee",
  jira_project: "project",
  jira_component: "component",
  jira_label: "label",
  jira_reporter: "reporter",
  jira_fix_versions: "fix version",
  jira_version: "version",
  jira_resolution: "resolution",
  jira_status_category: "status category"
};

export const azureIssueLeadTimeFilterOptionsMapping: Dict<string, string> = {
  issue_project: "project",
  issue_status: "status",
  issue_priority: "priority",
  issue_workitem_type: "workitem type",
  issue_status_category: "status category",
  issue_parent_workitem_id: "parent workitem id",
  issue_epic: "epic",
  issue_assignee: "assignee"
};

export const jiraTimeAcrossFilterOptionsMapping: Dict<string, string> = {
  ...jiraCommonFilterOptionsMapping,
  status: "Current Status"
};

export const scmTimeAcrossFilterOptionsMapping: Dict<string, string> = {
  version: "Affects Version",
  status: "Current Status"
};

export const scmCommonFilterOptionsMapping: Dict<string, string> = {
  label: "SCM Label",
  project: "Project Name",
  scm_file_repo_ids: "SCM FILE REPO ID",
  repo_ids: "REPO ID",
  jira_issue_type: "ISSUE TYPE",
  ...scmPRbrnachFilterMapping
};

export const scmPrsFilterOptionsMapping: Dict<string, string> = {
  label: "SCM Label",
  project: "Project",
  scm_file_repo_ids: "SCM FILE REPO ID",
  repo_ids: "REPO ID",
  jira_issue_type: "ISSUE TYPE",
  ...scmPRbrnachFilterMapping
};

export const scmCicdFilterOptionsMapping: Dict<string, string> = {
  repo: "SCM Repos",
  project_name: "Project",
  instance_name: "Instance Name",
  trend: "Trend",
  job_name: "Pipeline",
  cicd_user_id: "Triggered By",
  job_status: "Status",
  qualified_job_name: "Qualified Name",
  job_normalized_full_name: "Qualified Name",
  triage_rule: "Triage Rule",
  parameters: "Execution Parameters",
  job_end_date: "End Date",
  ...scmPRbrnachFilterMapping
};

export const coverityDefectFiltersMapping = {
  impacts: "cov_defect_impacts",
  categories: "cov_defect_categories",
  kinds: "cov_defect_kinds",
  checker_names: "cov_defect_checker_names",
  component_names: "cov_defect_component_names",
  types: "cov_defect_types",
  domains: "cov_defect_domains",
  first_detected_streams: "cov_defect_first_detected_streams",
  last_detected_streams: "cov_defect_last_detected_streams",
  file_paths: "cov_defect_file_paths",
  function_names: "cov_defect_function_names",
  last_detected_at: "cov_defect_last_detected_at",
  first_detected_at: "cov_defect_first_detected_at",
  first_detected: "cov_defect_first_detected",
  last_detected: "cov_defect_last_detected"
};
