import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { chartProps } from "../constant";

export const CHART_PROPS = {
  unit: "Hops",
  chartProps: chartProps
};

export const DEFAULT_QUERY = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
};

export const COMPOSITE_TRANSFORM = {
  min: "hops_min",
  median: "hops_median",
  max: "hops_max"
};

export const REPORT_NAME = "Issue Hops Report Trends";
export const URI = "issue_management_hops_report";
export const FILTERS = {
  across: "trend"
};
