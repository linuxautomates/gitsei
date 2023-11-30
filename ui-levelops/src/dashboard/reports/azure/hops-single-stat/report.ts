import { statReportTransformer } from "custom-hooks/helpers";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { azureDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import {
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping
} from "dashboard/constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { AzureHopsSingleStatReportType } from "model/report/azure/hops-single-stat/hops-single-stat.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  drillDownValuesToFiltersKeys,
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
} from "./constants";
import { IssueHopsSingleStatFiltersConfig } from "./filter.config";

const hopsSingleStatReport: { azure_hops_counts_stat: AzureHopsSingleStatReportType } = {
  azure_hops_counts_stat: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    xaxis: false,
    chart_props: CHART_PROPS,
    default_query: DEFAULT_QUERY,
    compareField: COMPARE_FIELD,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    transformFunction: statReportTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    valuesToFilters: drillDownValuesToFiltersKeys,
    supported_widget_types: SUPPORTED_WIDGET_TYPES,
    chart_click_enable: false,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: IssueHopsSingleStatFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};

export default hopsSingleStatReport;
