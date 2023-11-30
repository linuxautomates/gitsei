import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { chartProps } from "dashboard/reports/commonReports.constants";

export const jiraTicketsCountByFirstAssigneeReportChartTypes = {
  barProps: [
    {
      name: "total_tickets",
      dataKey: "total_tickets",
      unit: "Tickets"
    }
  ],
  stacked: false,
  unit: "Tickets",
  chartProps: chartProps
};

export const jiraTicketsCountByFirstAssigneeReportFilter = {
  across: "first_assignee"
};

export const jiraTicketsCountByFirstAssigneeReportDefaultQuery = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
};
