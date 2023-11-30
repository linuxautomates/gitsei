import moment from "moment";
import { AZURE_APPEND_ACROSS_OPTIONS } from "../constant";

export const ACROSS_OPTIONS = [
  { label: "PROJECT", value: "project" },
  { label: "NONE", value: "none" },
  { label: "STATUS", value: "status" },
  { label: "PRIORITY", value: "priority" },
  { label: "ASSIGNEE", value: "assignee" },
  { label: "REPORTER", value: "reporter" },
  { label: "WORKITEM TYPE", value: "workitem_type" },
  { label: "Azure Teams", value: "teams" },
  { label: "Azure Areas", value: "code_area" },
  { label: "Azure Iteration", value: "sprint" }
];

export const METRIC_OPTIONS = [
  { value: "median_time", label: "Median Time In Status" },
  { value: "average_time", label: "Average Time In Status" }
];

export const TIME_ACROSS_STAGES_REPORT_OPTIONS = [
  "none",
  "project",
  "status",
  "priority",
  "assignee",
  "reporter",
  "workitem_type"
];

export const ISSUE_TIME_ACROSS_BAR_PROPS = {
  barProps: [
    {
      name: "Median Time In Status",
      dataKey: "median_time"
    },

    {
      name: "Average Time In Status",
      dataKey: "average_time"
    }
  ],
  stacked: false,
  unit: "Days",
  sortBy: "median_time",
  chartProps: {
    barGap: 0,
    margin: { top: 20, right: 5, left: 5, bottom: 50 }
  }
};

export const TIME_ACROSS_STAGES_ID_FILTER = ["assignee", "reporter"];

export const azureAcrossStagesDefaultQuery = {
  metric: "median_time",
  // [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  workitem_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const REPORT_NAME = "Issue Time Across Stages";
export const URI = "issue_management_stage_time_report";
export const DATA_KEY = "median_time";
export const APPEND_ACROSS_OPTIONS = [...AZURE_APPEND_ACROSS_OPTIONS, { label: "Azure Iteration", value: "sprint" }];
export const DEFAULT_ACROSS = "none";
