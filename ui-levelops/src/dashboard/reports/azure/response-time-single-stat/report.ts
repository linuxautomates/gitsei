import { azureStatReportTransformerWrapper } from "custom-hooks/helpers/statReport.helper";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB
} from "dashboard/constants/filter-key.mapping";
import {
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping
} from "dashboard/constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { AzureResponseTimeSingleStatReportType } from "model/report/azure/response-time-single-stat/response-time-single-stat.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  REPORT_LIST_METHOD
} from "../constant";
import {
  CHART_PROPS,
  COMPARE_FIELD,
  DEFAULT_QUERY,
  FILTERS,
  REPORT_NAME,
  SUPPORTED_WIDGET_TYPES,
  URI
} from "./constant";
import { IssueResponseTimeSingleStatFiltersConfig } from "./filter.config";

const responseTimeSingleStat: { azure_response_time_counts_stat: AzureResponseTimeSingleStatReportType } = {
  azure_response_time_counts_stat: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    default_query: DEFAULT_QUERY,
    xaxis: false,
    chart_props: CHART_PROPS,
    drilldown: {},
    compareField: COMPARE_FIELD,
    supported_filters: issueManagementSupportedFilters,
    transformFunction: azureStatReportTransformerWrapper,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    supported_widget_types: SUPPORTED_WIDGET_TYPES,
    chart_click_enable: false,
    [HIDE_REPORT]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_SETTINGS_TAB]: true,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: IssueResponseTimeSingleStatFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};

export default responseTimeSingleStat;
