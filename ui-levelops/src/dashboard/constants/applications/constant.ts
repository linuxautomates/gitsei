import { jiraBussinessAlignmentDashboard } from "../bussiness-alignment-applications/jira-bussiness-alignment.application";
import jiraReports from "dashboard/reports/jira";
import { WidgetFilterType } from "../enums/WidgetFilterType.enum";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";

export const JIRA_WIDGETS_TYPE = Object.keys({ ...jiraReports, ...jiraBussinessAlignmentDashboard });

export const includeSolveTimeImplicitFilter = {
  include_solve_time: true
};

export const githubAppendAcrossOptions = [
  { label: "PR Created Week", value: "pr_created_week" },
  { label: "PR Created Month", value: "pr_created_month" },
  { label: "PR Created Quarter", value: "pr_created_quarter" },
  { label: "PR Closed Week", value: "pr_closed_week" },
  { label: "PR Closed Month", value: "pr_closed_month" },
  { label: "PR Closed Quarter", value: "pr_closed_quarter" },
  { label: "Code Change Size", value: "code_change" },
  { label: "PR Comment Density", value: "comment_density" },
  { label: "Approval Status", value: "approval_status" },
  { label: "Number of Approvers", value: "approver_count" },
  { label: "Number of Reviewers", value: "reviewer_count" },
  { label: "Approvers", value: "approver" },
  { label: "PR Labels", value: "label" },
  { label: "Source Branch", value: "source_branch" },
  { label: "Destination Branch", value: "target_branch" }
];

export const GITHUB_ISSUES_FIRST_RESPONSE_REPORT_TRENDS_KEY_MAPPING = {
  creators: "creator"
};

export const show_value_on_bar = {
  type: WidgetFilterType.CHECKBOX_BASED_FILTERS,
  label: "BAR CHART OPTIONS",
  BE_key: "show_value_on_bar",
  configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
  options: {
    checkboxLabel: "Show value above bar"
  }
};

export const CICD_DEPRECATED_MESSAGE = "This report is deprecated and is no more supported.";
export const XAXIS_TRUNCATE_LENGTH = "XAXIS_TRUNCATE_LENGTH";
