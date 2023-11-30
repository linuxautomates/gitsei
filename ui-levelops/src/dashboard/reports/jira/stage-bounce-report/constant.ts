import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { COMMON_ACROSS_OPTIONS } from "dashboard/report-filters/jira/common-filters.config";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { WidgetFilterType } from "../../../constants/enums/WidgetFilterType.enum";
import { WIDGET_DATA_SORT_FILTER_KEY } from "../../../constants/filter-name.mapping";
import { jiraSupportedFilters } from "../../../constants/supported-filters.constant";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "../../../constants/WidgetDataSortingFilter.constant";

export const stageBounceMetric = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Metric",
  BE_key: "metric",
  configTab: WIDGET_CONFIGURATION_KEYS.METRICS,
  defaultValue: "mean",
  options: [
    { value: "mean", label: "Mean Number of Times in stage" },
    { value: "median", label: "Median Number of Times in stage" },
    { value: "total_tickets", label: "Number of tickets" }
  ]
};

export const stageBounceChartProps = {
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

export const stageBounceDefaultQuery = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  stages: ["DONE"],
  metric: "mean"
};

export const stageBounceSupportedFilters = {
  ...jiraSupportedFilters,
  values: [...jiraSupportedFilters.values, "stage"]
};

export const stageBounceMetricOptions = [
  { value: "mean", label: "Mean Number of Times in stage" },
  { value: "median", label: "Median Number of Times in stage" },
  { value: "total_tickets", label: "Number of tickets" }
];

export const ACROSS_OPTIONS = [...COMMON_ACROSS_OPTIONS, { label: "Stage", value: "stage" }];

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
  { label: "Stage", value: "stage" }
];
