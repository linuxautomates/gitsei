import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { chartProps } from "../constant";

export const COMPOSITE_TRANSFORM = {
  min: "resolution_time_min",
  median: "resolution_time_median",
  max: "ressolution_time_max"
};

export const CHART_PROPS = {
  unit: "Days",
  chartProps: chartProps
};

export const DEFAULT_QUERY = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
};

export const REPORT_NAME = "Issues Resolution Time Trend Report";
export const URI = "issue_management_resolution_time_report";
export const FILTERS = {
  across: "trend"
};
