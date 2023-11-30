import { makeObjectKeyAsValue } from "utils/commonUtils";

export const SCM_COMMON_FILTER_KEY_MAPPING: Record<string, string> = {
  repo_id: "repo_ids",
  creator: "creators",
  state: "states",
  source_branch: "source_branches",
  target_branch: "target_branches",
  assignee: "assignees",
  reviewer: "reviewers",
  label: "labels",
  project: "projects",
  committer: "committers",
  column: "current_columns",
  author: "authors",
  branch: "branches",
  commit_branch: "commit_branches"
};

export const REVERSE_SCM_COMMON_FILTER_KEY_MAPPING: Record<string, string> =
  makeObjectKeyAsValue(SCM_COMMON_FILTER_KEY_MAPPING);

export const SCM_PRS_COMMON_FILTER_LABEL_MAPPING: Record<string, string> = {
  label: "SCM Label",
  target_branch: "Destination Branch"
};

export const SCM_COMMIT_COMMON_FILTER_LABEL_MAPPING: Record<string, string> = {
  project: "Project Name"
};

export const GITHUB_LEAD_TIME_FILTER_LABEL_MAPPING: Record<string, string> = {
  target_branch: "Destination Branch"
};

export const SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING: Record<string, string> = {
  project: "projects",
  creators: "creator",
  committers: "committer",
  authors: "author",
  assignees: "assignee",
  reviewers: "reviewer"
};

export const TIME_RANGE_OPTIONS = [
  {
    label: "Last day",
    value: 1
  },
  {
    label: "Last 7 days",
    value: 7
  },
  {
    label: "Last 2 Weeks",
    value: 14
  },
  {
    label: "Last 30 days",
    value: 30
  }
];

export const SCM_PR_BRNACH_FILTER_MAPPING = {
  source_branch: "Source Branch",
  target_branch: "Destination Branch"
};

export const SCM_FILTER_OPTIONS_MAPPING = {
  label: "SCM Label",
  project: "Project Name",
  scm_file_repo_ids: "SCM FILE REPO ID",
  repo_ids: "REPO ID",
  jira_issue_type: "ISSUE TYPE",
  ...SCM_PR_BRNACH_FILTER_MAPPING
};

export const GITHUB_PR_CREATED_AT = {
  key: "pr_created_at",
  label: "PR CREATED IN",
  dataKey: "pr_created_at",
  dataValueType: "string"
};

export const GITHUB_PR_CLOSED_AT = {
  key: "pr_closed_at",
  label: "PR CLOSED TIME",
  dataKey: "pr_closed_at",
  dataValueType: "string"
};

export const GITHUB_PR_MERGE_AT = {
  key: "pr_merged_at",
  label: "PR MERGED IN",
  dataKey: "pr_merged_at",
  dataValueType: "string"
};

export const GITHUB_PR_UPDATED_AT = {
  key: "pr_updated_at",
  label: "PR UPDATED IN",
  dataKey: "pr_updated_at",
  dataValueType: "string"
};

export const scmDoraSupportedFilters = {
  uri: "github_prs_filter_values",
  values: ["label", "project", "repo_id", "branch", "source_branch", "target_branch"]
};

export const BASE_SCM_CHART_PROPS = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

export const SCM_DRILLDOWN_VALUES_TO_FILTER = {
  code_change: "code_change_sizes"
};
