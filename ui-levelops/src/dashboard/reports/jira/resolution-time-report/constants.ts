import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import moment from "moment";
import { jiraChartProps, idFilters } from "../commonJiraReports.constants";
import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { basicMappingType } from "../../../dashboard-types/common-types";
import { get } from "lodash";
import { CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";

export const jiraResolutionTimeDefaultQuery = {
  metric: ["median_resolution_time", "number_of_tickets_closed"],
  [WIDGET_DATA_SORT_FILTER_KEY]:
    widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_METRIC_BASED],
  issue_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const resolutionTimeReportChartProps = {
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
  dataTruncatingValue: 2,
  stacked: false,
  unit: "Days",
  sortBy: "median",
  chartProps: jiraChartProps,
  xAxisIgnoreSortKeys: ["priority"]
};

export const resolutionTimeOnChartClickPayloadHandler = (args: { data: basicMappingType<any>; across?: string }) => {
  const { data, across } = args;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;
  if ((across && idFilters.includes(across)) || across?.includes(CUSTOM_FIELD_PREFIX)) {
    keyValue = {
      id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
      name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
    };
  }
  return keyValue;
};

export const ACROSS_OPTIONS = [
  { label: "Affects Version", value: "version" },
  { label: "ASSIGNEE", value: "assignee" },
  { label: "COMPONENT", value: "component" },
  { label: "FIX VERSION", value: "fix_version" },
  { label: "ISSUE TYPE", value: "issue_type" },
  { label: "LABEL", value: "label" },
  { label: "PRIORITY", value: "priority" },
  { label: "PROJECT", value: "project" },
  { label: "REPORTER", value: "reporter" },
  { label: "RESOLUTION", value: "resolution" },
  { label: "CURRENT STATUS", value: "status" },
  { label: "STATUS CATEGORY", value: "status_category" },
  { label: "Ticket Category", value: "ticket_category" },
  { label: "Issue Last Closed Week", value: "issue_resolved_week" },
  { label: "Issue Last Closed Month", value: "issue_resolved_month" },
  { label: "Issue Last Closed Quarter", value: "issue_resolved_quarter" }
];

export const includeSolveTimeImplicitFilter = {
  include_solve_time: true
};

export const WIDGET_ERROR_MESSAGE =
  "Incorrect configuration: Please either select a single metric or change the X-Axis sorting to By Label.";
