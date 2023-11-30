import { chartProps } from "../constant";

export const ACROSS_OPTIONS = [
  { label: "PROJECT", value: "project" },
  { label: "STATUS", value: "status" },
  { label: "PRIORITY", value: "priority" },
  { label: "ASSIGNEE", value: "assignee" },
  { label: "REPORTER", value: "reporter" },
  { label: "WORKITEM TYPE", value: "workitem_type" },
  { label: "TREND", value: "trend" },
  { label: "WORKITEM CREATED AT", value: "workitem_created_at" },
  { label: "WORKITEM UPDATED AT", value: "workitem_updated_at" },
  { label: "WORKITEM RESOLVED AT", value: "workitem_resolved_at" },
  { label: "Azure Teams", value: "teams" },
  { label: "Azure Areas", value: "code_area" },
  { label: "Azure Iteration", value: "sprint" }
];

export const CHART_PROPS = {
  barProps: [
    {
      name: "min",
      dataKey: "min"
    },
    {
      name: "median",
      dataKey: "median"
    },
    {
      name: "max",
      dataKey: "max"
    }
  ],
  stacked: false,
  unit: "Days",
  chartProps: chartProps,
  xAxisIgnoreSortKeys: ["priority"]
};

export const REPORT_NAME = "Issue Response Time Report";
export const DEFAULT_ACROSS = "assignee";
export const DEFAULT_FILTER_KEY = "median";
export const SHOW_EXTRA_INFO_ON_TOOLTIP = ["total_tickets"];
export const SORT_KEY = "response_time";
export const URI = "issue_management_response_time_report";
