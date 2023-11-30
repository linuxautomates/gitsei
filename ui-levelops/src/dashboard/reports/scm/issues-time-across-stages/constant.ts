import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = [
  { label: "Historical Status", value: "column" },
  { label: "Project", value: "project" },
  { label: "Issue Created By Date", value: "issue_created_day" },
  { label: "Issue Created By Week", value: "issue_created_week" },
  { label: "Issue Created By Month", value: "issue_created_month" },
  { label: "Repo", value: "repo_id" },
  { label: "Label", value: "label" },
  { label: "Assignee", value: "assignee" },
  { label: "Issue Closed By Date", value: "issue_closed_day" },
  { label: "Issue Closed By Week", value: "issue_closed_week" },
  { label: "Issue Closed By Month", value: "issue_closed_month" }
];

export const SCM_ISSUES_TIME_ACROSS_STAGES_LABEL_MAPPING = {
  column: "Current Status"
};

export const SCM_ISSUES_TIME_ACROSS_STAGES_REPORT = {
  barProps: [
    {
      name: "Median time in status",
      dataKey: "median_time"
    },
    {
      name: "Average time in stages",
      dataKey: "average_time"
    }
  ],
  stacked: false,
  unit: "Days",
  sortBy: "median_time",
  chartProps: BASE_SCM_CHART_PROPS
};
