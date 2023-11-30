import { statReportTransformer } from "custom-hooks/helpers";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { azureStatDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
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
import { AzureBounceSingleStatReportType } from "model/report/azure/bounce-single-stat/bounce-single-stat.model";
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
  SUPPORTED_WIDGETS_TYPES,
  URI
} from "./constants";
import { BounceSingleStatReportFiltersConfig } from "./filter.config";
import { xAxisLableTransform } from "./helper";

const bounceSingleStatReport: { azure_bounce_counts_stat: AzureBounceSingleStatReportType } = {
  azure_bounce_counts_stat: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    xaxis: false,
    chart_props: CHART_PROPS,
    default_query: DEFAULT_QUERY,
    compareField: COMPARE_FIELD,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureStatDrilldown,
    transformFunction: statReportTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    supported_widget_types: SUPPORTED_WIDGETS_TYPES,
    xAxisLabelTransform: xAxisLableTransform,
    chart_click_enable: false,
    [HIDE_REPORT]: true,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: BounceSingleStatReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};

export default bounceSingleStatReport;
