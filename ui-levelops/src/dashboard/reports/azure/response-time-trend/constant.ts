import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";

export const CHART_PROPS = {
  unit: "Days",
  chartProps: {
    barGap: 0,
    margin: { top: 20, right: 5, left: 5, bottom: 50 }
  }
};

export const DEFAULT_QUERY = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
};

export const FILTERS = {
  across: "trend"
};

export const COMPOSITE_TRANSFORMER = {
  min: "response_time_min",
  median: "response_time_median",
  max: "response_time_max"
};

export const REPORT_NAME = "Issue Response Time Report Trends";
export const URI = "issue_management_response_time_report";
