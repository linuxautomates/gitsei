import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
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
  { label: "WORKITEM RESOLVED AT", value: "workitem_resolved_at" }
];

export const DEFAULT_QUERY = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
};

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

export const REPORT_NAME = "Issue First Assignee Report";
export const DEFAULT_ACROSS = "assignee";
export const DEFAULT_FILTER_KEY = "median";
export const SHOW_EXTRA_INFO_ON_TOOLTIP = ["total_tickets"];
export const URI = "issue_management_first_assignee_report";
export const SORT_KEY = "assign_to_resolve";
