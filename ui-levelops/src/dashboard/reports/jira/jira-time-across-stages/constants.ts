import { chartProps } from "dashboard/reports/commonReports.constants";
import moment from "moment";

export const jiraAcrossStagesDefaultQuery = {
  metric: "median_time",
  // [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  issue_resolved_at: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const jiraAcrossStagesChartTypes = {
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
  chartProps: chartProps
};

export const jiraAcrossStagesIncludeSolveTimeImplicitFilter = {
  include_solve_time: true
};

export const ACROSS_OPTIONS = [
  { label: "AFFECTS VERSION", value: "version" },
  { label: "ASSIGNEE", value: "assignee" },
  { label: "COMPONENT", value: "component" },
  { label: "CURRENT STATUS", value: "status" },
  { label: "FIX VERSION", value: "fix_version" },
  { label: "ISSUE TYPE", value: "issue_type" },
  { label: "LABEL", value: "label" },
  { label: "PRIORITY", value: "priority" },
  { label: "PROJECT", value: "project" },
  { label: "REPORTER", value: "reporter" },
  { label: "RESOLUTION", value: "resolution" },
  { label: "STATUS CATEGORY", value: "status_category" },
  { label: "NONE", value: "none" },
  { label: "TICKET CATEGORY", value: "ticket_category" }
];
