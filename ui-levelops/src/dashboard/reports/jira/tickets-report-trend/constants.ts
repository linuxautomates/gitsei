import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { chartProps } from "dashboard/reports/commonReports.constants";

export const jiraTicketsReportTrendChartTypes = {
  unit: "Tickets",
  chartProps: chartProps
};
export const jiraTicketsReportTrendFilters = {
  across: "trend"
};

export const jiraTicketsReportTrendDefaultQuery = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
};
