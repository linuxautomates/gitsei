import { jiraResolutionTimeDataTransformer } from "custom-hooks/helpers";
import {
  IMPLICITY_INCLUDE_DRILLDOWN_FILTER,
  MULTI_SERIES_REPORT_FILTERS_CONFIG,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  jiraTimeAcrossFilterOptionsMapping,
  VALUE_SORT_KEY,
  WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE
} from "dashboard/constants/filter-name.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ResolutionTimeReportType } from "model/report/jira/resolution-time-report/resolutionTimeReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import {
  includeSolveTimeImplicitFilter,
  jiraResolutionTimeDefaultQuery,
  resolutionTimeReportChartProps
} from "./constants";
import { JiraResolutionTimeReportFiltersConfig } from "./filters.config";
import { resolutionTimeOnChartClickPayloadHandler, resolutionTimeReportChartUnits, validateWidget } from "./helper";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { JiraMultiSeriesResolutionTimeReportFiltersConfig } from "dashboard/reports/multiseries-reports/jira/issue-resolution-time-report/filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraResolutionTimeReport: { resolution_time_report: ResolutionTimeReportType } = {
  resolution_time_report: {
    name: "Issue Resolution Time Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "assignee",
    default_query: jiraResolutionTimeDefaultQuery,
    xaxis: true,
    chart_props: resolutionTimeReportChartProps,
    tooltipMapping: { number_of_tickets_closed: "Number of Tickets" },
    uri: "resolution_time_report",
    method: "list",
    filters: {},
    dataKey: ["median_resolution_time", "number_of_tickets_closed"],
    supported_filters: jiraSupportedFilters,
    convertTo: "days",
    drilldown: jiraDrilldown,
    weekStartsOnMonday: true,
    [FILTER_NAME_MAPPING]: jiraTimeAcrossFilterOptionsMapping,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    xAxisLabelTransform: getXAxisLabel,
    onChartClickPayload: resolutionTimeOnChartClickPayloadHandler,
    getChartUnits: resolutionTimeReportChartUnits,
    transformFunction: data => jiraResolutionTimeDataTransformer(data),
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "resolution_time",
    [REPORT_FILTERS_CONFIG]: JiraResolutionTimeReportFiltersConfig,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: JiraMultiSeriesResolutionTimeReportFiltersConfig,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE]: validateWidget
  }
};

export default jiraResolutionTimeReport;
