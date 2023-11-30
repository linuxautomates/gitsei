import { trendReportTransformer } from "custom-hooks/helpers";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
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
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { AzureBounceTrendReportType } from "model/report/azure/bounce-report-trend/bounce-report-trend.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  drillDownValuesToFiltersKeys,
  REPORT_LIST_METHOD
} from "../constant";
import { CHART_PROPS, COMPOSITE_TRANSFORM, DEFAULT_QUERY, FILTERS, REPORT_NAME, URI } from "./constants";
import { IssuesBounceReportTrendsFiltersConfig } from "./filter.config";

const bounceTrendReport: { azure_bounce_report_trends: AzureBounceTrendReportType } = {
  azure_bounce_report_trends: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: CHART_PROPS,
    uri: URI,
    composite: true,
    composite_transform: COMPOSITE_TRANSFORM,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    default_query: DEFAULT_QUERY,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [HIDE_REPORT]: true,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    transformFunction: trendReportTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [REPORT_FILTERS_CONFIG]: IssuesBounceReportTrendsFiltersConfig,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};
export default bounceTrendReport;
