import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = [
  { label: "APPROVAL STATUS", value: "approval_status" },
  { label: "APPROVERS", value: "approver" },
  { label: "ASSIGNEE", value: "assignee" },
  { label: "CODE CHANGE SIZE", value: "code_change" },
  { label: "CREATOR", value: "creator" },
  { label: "NUMBER OF APPROVERS", value: "approver_count" },
  { label: "NUMBER OF REVIEWERS", value: "reviewer_count" },
  { label: "PR CLOSED MONTH", value: "pr_closed_month" },
  { label: "PR CLOSED QUARTER", value: "pr_closed_quarter" },
  { label: "PR CLOSED WEEK", value: "pr_closed_week" },
  { label: "PR COMMENT DENSITY", value: "comment_density" },
  { label: "PR CREATED MONTH", value: "pr_created_month" },
  { label: "PR CREATED QUARTER", value: "pr_created_quarter" },
  { label: "PR CREATED WEEK", value: "pr_created_week" },
  { label: "PR LABELS", value: "label" },
  { label: "PROJECT", value: "project" },
  { label: "REPO ID", value: "repo_id" },
  { label: "REVIEWER", value: "reviewer" },
  { label: "STATE", value: "state" },
  { label: "SOURCE BRANCH", value: "source_branch" },
  { label: "DESTINATION BRANCH", value: "target_branch" }
];

export const VISUALIZATION_OPTIONS = [
  { label: "Pie Chart", value: IssueVisualizationTypes.PIE_CHART },
  { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
  { label: "Line Chart", value: IssueVisualizationTypes.LINE_CHART },
  { label: "Smooth Area Chart", value: IssueVisualizationTypes.AREA_CHART },
  { label: "Stacked Smooth Area Chart", value: IssueVisualizationTypes.STACKED_AREA_CHART }
];

export const SCM_PRS_REPORT_DESCRIPTION =
  "Cumulative count of pull requests for each of the repositories for the selected duration.";
export const SCM_PRS_CHART_PROPS = {
  unit: "Count",
  barProps: [
    {
      name: "count",
      dataKey: "count",
      unit: "count"
    }
  ],
  stacked: false,
  chartProps: BASE_SCM_CHART_PROPS
};

export const SCM_PRS_API_BASED_FILTERS = ["committers", "authors", "creators", "assignees", "reviewers"];
export const SCM_PRS_FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS = ["labels"];

export const SCM_PRS_STACK_FILTER = [
  "approval_status",
  "approver",
  "assignee",
  "code_change",
  "creator",
  "approver_count",
  "reviewer_count",
  "pr_closed_month",
  "pr_closed_quarter",
  "pr_closed_week",
  "comment_density",
  "pr_created_month",
  "pr_created_quarter",
  "pr_created_week",
  "label",
  "project",
  "repo_id",
  "reviewer",
  "state",
  "source_branch",
  "target_branch"
];