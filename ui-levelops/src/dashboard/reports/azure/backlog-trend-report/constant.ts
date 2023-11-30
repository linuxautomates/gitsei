import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { chartProps } from "../constant";

export const STACK_OPTIONS = [
  { label: "Affects Version", value: "version" },
  { label: "assignee", value: "assignee" },
  { label: "component", value: "component" },
  { label: "fix_version", value: "fix_version" },
  { label: "issue_type", value: "issue_type" },
  { label: "label", value: "label" },
  { label: "priority", value: "priority" },
  { label: "project", value: "project" },
  { label: "reporter", value: "reporter" },
  { label: "resolution", value: "resolution" },
  { label: "status", value: "status" },
  { label: "status_category", value: "status_category" },
  { label: "workitem_type", value: "workitem_type" },
  { label: "parent_workitem_id", value: "parent_workitem_id" },
  { label: "epic", value: "epic" },
  { label: "story_points", value: "story_points" },
  { label: "code_area", value: "code_area" },
  { label: "teams", value: "teams" }
];

export const CHART_PROPS = {
  barProps: [
    {
      name: "median",
      dataKey: "median"
    }
  ],
  stacked: false,
  unit: "Days",
  chartProps: chartProps,
  xAxisProps: {
    interval: 0
  }
};

export const STACKS_FILTERS = [
  "project",
  "status",
  "priority",
  "workitem_type",
  "status_category",
  "parent_workitem_id",
  "epic",
  "assignee",
  "ticket_category",
  "version",
  "fix_version",
  "reporter",
  "label",
  "story_points",
  "teams",
  "code_area"
];

export const DEFAULT_QUERY = {
  across: "trend",
  interval: "week",
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
};

export const FILTERS = {
  across: "trend"
};

export const URI = "issue_management_age_report";

export const REPORT_NAME = "Issue Backlog Trend Report";
