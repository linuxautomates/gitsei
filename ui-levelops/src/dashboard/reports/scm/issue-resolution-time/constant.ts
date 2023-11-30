import { BASE_SCM_CHART_PROPS } from "../constant";

export const ACROSS_OPTIONS = [
  { label: "Project", value: "project" },
  { label: "Label", value: "label" },
  { label: "Repo", value: "repo_id" },
  { label: "Assignee", value: "assignee" },
  { label: "Issue Created By Date", value: "issue_created" },
  { label: "Issue Updated By Date", value: "issue_updated" },
  { label: "Issue Last Closed Week", value: "issue_created_week" },
  { label: "Issue Last Closed Month", value: "issue_created_month" },
  { label: "Issue Last Closed Quarter", value: "issue_created_quarter" }
];

export const METRIC_OPTIONS = [
  { value: "median_resolution_time", label: "Median Resolution Time" },
  { value: "number_of_tickets_closed", label: "Number Of Tickets" }
];

export const SCM_ISSUES_RESOLUTION_TIME_CHART_PROPS = {
  barProps: [
    {
      name: "Median Resolution Time",
      dataKey: "median_resolution_time"
    },
    {
      name: "Number of Tickets",
      dataKey: "number_of_tickets_closed"
    }
  ],
  stacked: false,
  unit: "Days",
  sortBy: "median_resolution_time",
  chartProps: BASE_SCM_CHART_PROPS
};

export const SCM_RESOLUTION_TIME_API_BASED_FILTERS = ["creators", "committers", "authors", "assignees", "reviewers"];
export const SCM_RESOLUTION_TIME_DATA_KEYS = ["median_resolution_time", "number_of_tickets_closed"];
