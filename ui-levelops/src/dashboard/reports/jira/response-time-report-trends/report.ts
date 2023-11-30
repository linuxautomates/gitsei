import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER, REPORT_FILTERS_CONFIG } from "../../../constants/applications/names";
import { trendReportTransformer } from "custom-hooks/helpers";
import { ChartContainerType } from "dashboard/helpers/helper";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { jiraResponseTimeTrendDrilldown } from "dashboard/constants/drilldown.constants";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ResponseTimeReportTrendsType } from "model/report/jira/response-time-report-trends/responseTimeReportTrends.constants";
import { jiraApiBasedFilterKeyMapping, jiraChartProps } from "../commonJiraReports.constants";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  WIDGET_DATA_SORT_FILTER_KEY
} from "dashboard/constants/filter-name.mapping";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { JiraResponseTimeReportTrendsFiltersConfig } from "./filters.config";
import { includeSolveTimeImplicitFilter, JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraResponseTimeReportTrends: { response_time_report_trends: ResponseTimeReportTrendsType } = {
  response_time_report_trends: {
    name: "Issue Response Time Report Trends",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "response_time_min",
      median: "response_time_median",
      max: "response_time_max"
    },
    chart_props: {
      unit: "Days",
      chartProps: jiraChartProps
    },
    uri: "response_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    supported_filters: jiraSupportedFilters,
    convertTo: "days",
    drilldown: jiraResponseTimeTrendDrilldown,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    transformFunction: (data: any) => trendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: JiraResponseTimeReportTrendsFiltersConfig,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default jiraResponseTimeReportTrends;
