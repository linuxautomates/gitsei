import moment from "moment";
import { chartProps } from "../constant";
import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";

export const ACROSS_OPTIONS = [
  { label: "ASSIGNEE", value: "assignee" },
  { label: "Azure Areas", value: "code_area" },
  { label: "Azure Iteration", value: "sprint" },
  { label: "AZURE STORY POINTS", value: "story_points" },
  { label: "AZURE TEAMS", value: "teams" },
  { label: "WORKITEM CREATED BY MONTH", value: "workitem_created_at_month" },
  { label: "WORKITEM CREATED BY QUARTER", value: "workitem_created_at_quarter" },
  { label: "WORKITEM CREATED BY WEEK", value: "workitem_created_at_week" },
  { label: "WORKITEM RESOLVED BY MONTH", value: "workitem_resolved_at_month" },
  { label: "WORKITEM RESOLVED BY QUARTER", value: "workitem_resolved_at_quarter" },
  { label: "WORKITEM RESOLVED BY WEEK", value: "workitem_resolved_at_week" },
  { label: "WORKITEM UPDATED BY MONTH", value: "workitem_updated_at_month" },
  { label: "WORKITEM UPDATED BY QUARTER", value: "workitem_updated_at_quarter" },
  { label: "WORKITEM UPDATED BY WEEK", value: "workitem_updated_at_week" },
  { label: "PRIORITY", value: "priority" },
  { label: "PROJECT", value: "project" },
  { label: "REPORTER", value: "reporter" },
  { label: "STATUS", value: "status" },
  { label: "TICKET CATEGORY", value: "ticket_category" },
  { label: "TREND", value: "trend" },
  { label: "WORKITEM TYPE", value: "workitem_type" }
];

export const METRIC_OPTIONS = [
  { value: "median_resolution_time", label: "Median Resolution Time" },
  { value: "number_of_tickets_closed", label: "Number Of Tickets" },
  { value: "90th_percentile_resolution_time", label: "90th Percentile Resolution Time" },
  { value: "average_resolution_time", label: "Average Resolution Time" }
];

export const DEFAULT_QUERY = {
  metric: ["median_resolution_time", "number_of_tickets_closed"],
  [WIDGET_DATA_SORT_FILTER_KEY]:
    widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_METRIC_BASED],
  workitem_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const CHART_PROPS = {
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
  chartProps: chartProps,
  xAxisIgnoreSortKeys: ["priority"]
};

export const REPORT_NAME = "Issue Resolution Time Report";
export const DEFAULT_ACROSS = "assignee";
export const TOOLTIP_MAPPING = { number_of_tickets_closed: "Number of Tickets" };
export const URI = "issue_management_resolution_time_report";
export const DATA_KEY = ["median_resolution_time", "number_of_tickets_closed"];
export const FILTERS_KEY_MAPPING = {
  ticket_categories: "workitem_ticket_categories",
  ticket_categorization_scheme: "workitem_ticket_categorization_scheme"
};
export const SORT_KEY = "resolution_time";
export const WIDGET_ERROR_MESSAGE =
  "Incorrect configuration: Please either select a single metric or change the X-Axis sorting to By Label.";
