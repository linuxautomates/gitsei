import { azureResolutionTimeDataTransformer } from "custom-hooks/helpers/seriesData.helper";
import {
  IMPLICITY_INCLUDE_DRILLDOWN_FILTER,
  MULTI_SERIES_REPORT_FILTERS_CONFIG,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG,
  TRANSFORM_LEGEND_DATAKEY
} from "dashboard/constants/applications/names";
import { azureDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  FILTER_KEY_MAPPING,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping,
  VALUE_SORT_KEY,
  WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE
} from "dashboard/constants/filter-name.mapping";
import { azureExcludeStatusFilter, FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { AzureMultiSeriesResolutionTimeReportFiltersConfig } from "dashboard/reports/multiseries-reports/azure/resolution-time-report/filter.config";
import { AzureResolutionTimeReportType } from "model/report/azure/resolution-time-report/resolution-time-report.model";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_IMPLICIT_FILTER,
  drillDownValuesToFiltersKeys,
  REPORT_LIST_METHOD
} from "../constant";
import {
  ACROSS_OPTIONS,
  CHART_PROPS,
  DATA_KEY,
  DEFAULT_ACROSS,
  DEFAULT_QUERY,
  FILTERS_KEY_MAPPING,
  REPORT_NAME,
  SORT_KEY,
  TOOLTIP_MAPPING,
  URI
} from "./constant";
import { ResolutionTimeReportFiltersConfig } from "./filter.config";
import { onChartClick, validateWidget } from "./helper";
import { transformDataKey } from "shared-resources/charts/helper";

const resolutionTimeReport: { azure_resolution_time_report: AzureResolutionTimeReportType } = {
  azure_resolution_time_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.AZURE_RESOLUTION_TIME,
    across: ACROSS_OPTIONS.map((item: { label: string; value: string }) => item.value),
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: DEFAULT_ACROSS,
    default_query: DEFAULT_QUERY,
    xaxis: true,
    chart_props: CHART_PROPS,
    tooltipMapping: TOOLTIP_MAPPING,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    dataKey: DATA_KEY,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    convertTo: "days",
    drilldown: azureDrilldown,
    xAxisLabelTransform: getXAxisLabel,
    onChartClickPayload: onChartClick,
    transformFunction: azureResolutionTimeDataTransformer,
    weekStartsOnMonday: true,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: SORT_KEY,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    [FILTER_WITH_INFO_MAPPING]: [azureExcludeStatusFilter],
    valuesToFilters: drillDownValuesToFiltersKeys,
    [FILTER_KEY_MAPPING]: FILTERS_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: ResolutionTimeReportFiltersConfig,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: AZURE_IMPLICIT_FILTER,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: AzureMultiSeriesResolutionTimeReportFiltersConfig,
    [TRANSFORM_LEGEND_DATAKEY]: transformDataKey,
    [WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE]: validateWidget
  }
};

export default resolutionTimeReport;
