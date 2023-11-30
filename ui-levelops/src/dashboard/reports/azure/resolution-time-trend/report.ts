import { azureTrendTransformer } from "custom-hooks/helpers/trendReport.helper";
import {
  IMPLICITY_INCLUDE_DRILLDOWN_FILTER,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { azureDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping
} from "dashboard/constants/filter-name.mapping";
import { azureExcludeStatusFilter, FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { AzureResolutionTimeTrendReportType } from "model/report/azure/resolution-time-trend-report/resolution-time-trend-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_IMPLICIT_FILTER,
  drillDownValuesToFiltersKeys,
  REPORT_LIST_METHOD
} from "../constant";
import { CHART_PROPS, COMPOSITE_TRANSFORM, DEFAULT_QUERY, FILTERS, REPORT_NAME, URI } from "./constants";
import { ResolutionTimeTrendsReportFiltersConfig } from "./filter.config";

const resolutionTimeTrendReport: { azure_resolution_time_report_trends: AzureResolutionTimeTrendReportType } = {
  azure_resolution_time_report_trends: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    xaxis: false,
    composite: true,
    composite_transform: COMPOSITE_TRANSFORM,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: CHART_PROPS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    default_query: DEFAULT_QUERY,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    convertTo: "days",
    drilldown: azureDrilldown,
    transformFunction: azureTrendTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [azureExcludeStatusFilter],
    [HIDE_REPORT]: true,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: AZURE_IMPLICIT_FILTER,
    [REPORT_FILTERS_CONFIG]: ResolutionTimeTrendsReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};

export default resolutionTimeTrendReport;
