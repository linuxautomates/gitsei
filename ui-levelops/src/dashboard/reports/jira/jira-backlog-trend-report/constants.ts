import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { chartProps } from "dashboard/reports/commonReports.constants";
import moment from "moment";

export const jiraBacklogTrendReportChartTypes = {
  barProps: [
    {
      name: "median",
      dataKey: "median"
    }
  ],
  stacked: false,
  unit: "Days",
  // sortBy: "median",
  chartProps: chartProps
};

export const jiraBacklogTrendDefaultQuery = {
  interval: "week",
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED],
  snapshot_range: {
    $gt: moment.utc().subtract(3, "weeks").startOf("week").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const ACROSS_OPTIONS = [
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
  { label: "status_category", value: "status_category" }
];
