import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { chartProps } from "../constant";

export const COMPOSITE_TRANSFORM = {
  min: "bounce_min",
  median: "bounce_median",
  max: "bounce_max"
};

export const CHART_PROPS = {
  unit: "Bounces",
  chartProps: chartProps
};

export const DEFAULT_QUERY = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
};

export const REPORT_NAME = "Issue Bounce Report Trends";

export const URI = "issue_management_bounce_report";

export const FILTERS = {
  across: "trend"
};
