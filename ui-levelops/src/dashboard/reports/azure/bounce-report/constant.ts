import { azureDrilldown } from "dashboard/constants/drilldown.constants";
import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";

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

export const DRILL_DOWN_VISIBLE_COLUMN = ["workitem_id", "summary", "components", "bounces", "hops", "assignee"];

export const DEFAULT_QUERY = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
};

export const REPORT_NAME = "Issue Bounce Report";

export const CHART_PROPS = {
  yDataKey: "median",
  rangeY: ["min", "max"],
  unit: "Bounces",
  // When we do not want to sort the data for particular across value add across value in the array
  xAxisIgnoreSortKeys: ["priority"]
};

export const DEFAULT_SORT = [{ id: "bounces", desc: true }];

export const URI = "issue_management_bounce_report";

export const DRILL_DOWN = {
  ...azureDrilldown,
  drilldownVisibleColumn: DRILL_DOWN_VISIBLE_COLUMN
};

export const DEFAULT_ACROSS = "assignee";
export const SORT_KEY = "bounces";
