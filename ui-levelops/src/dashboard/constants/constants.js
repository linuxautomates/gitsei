import { jiraBAReportTypes } from "./enums/jira-ba-reports.enum";
import { ISSUE_MANAGEMENT_REPORTS, JIRA_MANAGEMENT_TICKET_REPORT } from "./applications/names";

export const GROUP_BY_ROOT_FOLDER = "groupByRootFolder";

export const valuesToFilters = {
  status: "statuses",
  priority: "priorities",
  issue_type: "issue_types",
  assignee: "assignees",
  project: "projects",
  category: "category", // Used by microsoft issues report
  model: "model", // Used by microsoft issues report
  component: "components",
  hygiene_type: "hygiene_types",
  repo_id: "repo_ids",
  scm_file_repo_id: "scm_file_repo_ids",
  creator: "creators",
  state: "states",
  branch: "branches",
  label: "labels",
  reviewer: "reviewers",
  committer: "committers",
  author: "authors",
  cicd_user_id: "cicd_user_ids",
  job_status: "job_statuses",
  job_name: "job_names",
  time_period: "time_period",
  agg_type: "agg_type",
  parameters: "parameters",
  jenkins_filter_type: "jenkins_filter_type",
  agg_name: "agg_name",
  type: "types",
  brand: "brands",
  labels: "labels",
  stacks: "stacks",
  across: "across",
  contact: "contacts",
  account: "accounts",
  organization: "organizations",
  requester: "requesters",
  submitter: "submitters",
  qualified_job_name: "qualified_job_name",
  cicd_job_id: "cicd_job_ids",
  incident_priority: "incident_priority",
  incident_urgency: "incident_urgency",
  alert_severity: "alert_severity",
  poor_description: "poor_description",
  late_attachments: "late_attachments",
  idle: "idle",
  reporter: "reporters",
  service: "service",
  jira_status: "jira_statuses",
  jira_priority: "jira_priorities",
  jira_assignee: "jira_assignees",
  jira_project: "jira_projects",
  jira_issue_type: "jira_issue_types",
  jira_component: "jira_components",
  jira_label: "jira_labels",
  jira_reporter: "jira_reporters",
  jira_epic: "jira_epics",
  jira_status_category: "jira_status_categories",
  zendesk_brand: "zendesk_brands",
  zendesk_type: "zendesk_types",
  zendesk_priority: "zendesk_priorities",
  zendesk_status: "zendesk_statuses",
  zendesk_organization: "zendesk_organizations",
  zendesk_assignee: "zendesk_assignees",
  zendesk_requester: "zendesk_requesters",
  zendesk_submitter: "zendesk_submitters",
  epic: "epics",
  salesforce_type: "salesforce_types",
  salesforce_contact: "salesforce_contacts",
  salesforce_priority: "salesforce_priorities",
  salesforce_status: "salesforce_statuses",
  account_name: "accounts",
  user_id: "user_ids",
  pd_service: "pd_service_id",
  fix_version: "fix_versions",
  version: "versions",
  test_status: "test_statuses",
  test_run: "test_runs",
  test_plan: "test_plans",
  milestone: "milestones",
  test_suite: "test_suites",
  severity: "severities",
  risk: "risk",
  report_grade: "report_grade",
  report_security: "report_security",
  stages: "stages",
  result: "results", // For Azure
  pipeline: "pipelines", // For Azure
  instance_name: "instance_names",
  resolution: "resolutions",
  status_category: "status_categories",
  jenkins_job_path: "job_normalized_full_names",
  job_normalized_full_name: "job_normalized_full_names",
  first_assignee: "first_assignees",
  project_name: "projects",
  repo: "repos",
  ticket_category: "ticket_categories", // TODO subjected to changes
  column: "columns",
  current_column: "current_columns",
  card_creator: "card_creators",
  parent_workitem_id: "parent_workitem_ids",
  workitem_type: "workitem_types",
  issue_project: "issue_projects",
  issue_status: "issue_statuses",
  issue_priority: "issue_priorities",
  issue_workitem_type: "issue_workitem_types",
  issue_status_category: "issue_status_categories",
  issue_parent_workitem_id: "issue_parent_workitem_ids",
  issue_epic: "issue_epics",
  ticket_categorization_scheme: "ticket_categorization_scheme",
  sprint_id: "sprint_ids",
  issue_assignee: "issue_assignees",
  workitem_project: "workitem_projects",
  workitem_status: "workitem_statuses",
  workitem_priority: "workitem_priorities",
  workitem_status_category: "workitem_status_categories",
  workitem_parent_workitem_id: "workitem_parent_workitem_ids",
  workitem_epic: "workitem_epics",
  workitem_assignee: "workitem_assignees",
  workitem_ticket_category: "workitem_ticket_categories",
  workitem_version: "workitem_versions",
  workitem_fix_version: "workitem_fix_versions",
  workitem_reporter: "workitem_reporters",
  workitem_label: "workitem_labels",
  file_type: "file_types",
  num_reviewer: "num_reviewers",
  num_approver: "num_approvers",
  code_change_size: "code_change_sizes",
  comment_density: "comment_densities",
  approval_status: "approval_statuses",
  approver: "approvers",
  commenter: "commenters",
  review_type: "review_types",
  technology: "technologies",
  target_branch: "target_branches",
  source_branch: "source_branches",
  commit_branch: "commit_branches",
  rollback: "rollback",
  environment: "environments",
  infrastructure: "infrastructures",
  deployment_type: "deployment_types",
  repository: "repositories",
  tag: "tags"
};

export const USE_PAGINATED_FILTERS_THRESHOLD = 30;
export const DEFAULT_MAX_STACKED_ENTRIES = 10;
export const DEFAULT_MAX_RECORDS = 20;

export const CUSTOM_FIELD_PREFIX = "customfield_";
export const AZURE_CUSTOM_FIELD_PREFIX = "Custom.";
export const AZURE_ISSUE_CUSTOM_FIELD_PREFIX = "Microsoft.VSTS.";
export const TESTRAILS_CUSTOM_FIELD_PREFIX = "custom_";
export const CUSTOM_FIELD_STACK_FLAG = "custom_field";
export const CUSTOM_STACK_FILTER_NAME_MAPPING = "custom_stack_filter_name_mapping"; // for storing name of custom stack selected
export const CUSTOM_FIELD_NAME_STORY_POINTS = "story points";
export const STARTS_WITH = "$begins";
export const CONTAINS = "$contains";
export const REGEX = "$regex";

export const scmMergeTrendTypes = [
  "github_prs_merge_trends",
  "github_prs_first_review_trends",
  "github_prs_first_review_to_merge_trends",
  "github_prs_merge_single_stat",
  "github_prs_first_review_single_stat",
  "github_prs_first_review_to_merge_single_stat"
];

export const defaultWeights = {
  IDLE: 20,
  POOR_DESCRIPTION: 8,
  NO_DUE_DATE: 30,
  NO_ASSIGNEE: 20,
  NO_COMPONENTS: 20
};

export const zendeskHygieneDefaultWeights = {
  IDLE: 20,
  POOR_DESCRIPTION: 8,
  NO_CONTACT: 20
};

export const jenkinsJobReports = [
  "cicd_pipeline_jobs_duration_report",
  "cicd_pipeline_jobs_duration_trend_report",
  "cicd_pipeline_jobs_count_report",
  "cicd_pipeline_jobs_count_trend_report",
  "cicd_scm_jobs_count_report"
];

export const bullseyeJobReports = [
  "bullseye_function_coverage_report",
  "bullseye_branch_coverage_report",
  "bullseye_decision_coverage_report",
  "bullseye_code_coverage_report",
  "bullseye_function_coverage_trend_report",
  "bullseye_branch_coverage_trend_report",
  "bullseye_decision_coverage_trend_report",
  "bullseye_code_coverage_trend_report"
];

export const jiraTicketsTrendReportOptions = [
  {
    label: "Trend",
    value: "trend"
  },
  {
    label: "Issue Created",
    value: "issue_created"
  },
  {
    label: "Issue Updated",
    value: "issue_updated"
  }
];

export const sprintReportXAxisOptions = [
  {
    label: "Weekly by Sprint end date",
    value: "week"
  },
  {
    label: "Bi-weekly by Sprint end date",
    value: "bi_week"
  },
  {
    label: "Monthly",
    value: "month"
  },
  {
    label: "Sprint",
    value: "sprint"
  }
];

export const leadTimeReportXAxisOptions = [
  {
    label: "Type",
    value: "issue_type"
  },
  {
    label: "Effort Category",
    value: "effort_category"
  }
];

// eslint-disable-next-line no-unused-vars
const getValueForFilter = filterName => {
  for (const [key, value] of Object.entries(valuesToFilters)) {
    if (value === filterName) {
      return key;
    }
  }
};

// TODO: discuss it 90 * 5 days in seconds.
export const TREND_REPORT_TIME_RANGE_LIMIT = 7776000 * 5;

export const DEFAULT_DASHBOARD_KEY = "DEFAULT_DASHBOARD_KEY";
export const NO_DASH = "no-dash";
export const NO_DEFAULT_DASH_ID = `?default=${NO_DASH}`;

//tenant state
export const TENANT_STATE = "tenant_state";

export const SPRINT_FILTER_META_KEY = "sprint_filter_key";

// Removing assignee as showing ids in place of names
export const ADDITIONAL_KEY_FILTERS = [
  "creator",
  "committer",
  "author",
  "reviewer",
  "assignee",
  "reporter",
  "jira_assignee",
  "jira_reporter",
  "workitem_assignee",
  "workitem_reporter"
];
export const DASHBOARDS_TITLE = "Insights";
export const MANAGE_DASHBOARDS_BUTTON_TITLE = "Manage Insights";
export const DASHBOARD_SEARCH_PLACEHOLDER = "Search Insights";
export const DASHBOARD_LIST_COUNT = "DASH_LIST_COUNT";
export const SECURITY = "security";

export const reportsHavingTicketCategoryDrilldownCol = [
  jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT
];

export const CUSTOM_STORY_POINTS_LABEL = "Story Points";

export const IGNORED_ADDITIONAL_KEY_REPORTS_FOR_DRILLDOWN_TITLE = [
  "azure_hygiene_report",
  "hygiene_report",
  JIRA_MANAGEMENT_TICKET_REPORT.HYGIENE_REPORT_TREND,
  ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT_TREND
];
