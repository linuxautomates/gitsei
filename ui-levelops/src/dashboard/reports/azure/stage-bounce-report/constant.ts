import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { chartProps, drillDownValuesToFiltersKeys } from "../constant";

export const METRIC_OPTIONS = [
  { value: "mean", label: "Mean Number of Times in stage" },
  { value: "median", label: "Median Number of Times in stage" },
  { value: "total_tickets", label: "Number of tickets" }
];

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
  { label: "Stage", value: "stage" }
];

export const STACK_OPTIONS = [
  { label: "PROJECT", value: "project" },
  { label: "STATUS", value: "status" },
  { label: "PRIORITY", value: "priority" },
  { label: "ASSIGNEE", value: "assignee" },
  { label: "REPORTER", value: "reporter" },
  { label: "WORKITEM TYPE", value: "workitem_type" },
  { label: "TREND", value: "trend" },
  { label: "Stage", value: "stage" }
];

export const AZURE_ACROSS_OPTION = [
  "project",
  "status",
  "priority",
  "assignee",
  "reporter",
  "workitem_type",
  "trend",
  "workitem_created_at",
  "workitem_updated_at",
  "workitem_resolved_at",
  "stage"
];

export const CHART_PROPS = {
  barProps: [
    {
      name: "total_tickets",
      dataKey: "total_tickets",
      unit: "Tickets"
    }
  ],
  stacked: false,
  unit: "Tickets",
  sortBy: "total_tickets",
  chartProps: chartProps
};

export const DEFAULT_QUERY = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  workitem_stages: ["Done"],
  metric: "mean"
};

export const STACK_FILTERS = ["project", "status", "priority", "assignee", "reporter", "workitem_type", "stage"];

export const TOOLTIP_MAPPING = {
  mean: "Mean Number of Times in stage",
  median: "Median Number of Times in stage",
  total_tickets: "Number of tickets"
};

export const REPORT_NAME = "Stage Bounce Report";
export const DEFAULT_ACROSS = "stage";
export const URI = "issue_management_stage_bounce_report";
export const REQUIRED_FILTERS = ["workitem_stage"];
export const INFORMATION_MESSAGE = {
  stacks_disabled: "Stacks option is not applicable"
};
export const SUPPORTED_FILTERS = {
  ...issueManagementSupportedFilters,
  values: [...issueManagementSupportedFilters.values, "workitem_stage"]
};

export const VALUES_TO_FILTERS = {
  ...drillDownValuesToFiltersKeys,
  stage: "workitem_stages",
  workitem_stage: "workitem_stages"
};
