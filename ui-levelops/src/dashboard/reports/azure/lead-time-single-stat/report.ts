import { azureLeadTimeStatReportTransformer } from "custom-hooks/helpers/azureleadTimeSingleStatTransformer";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { leadTimeSingleStatAzureSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { AzureLeadTimeStatReportType } from "model/report/azure/lead-time-single-stat/lead-time-single-stat.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  COMMON_FILTER_OPTIONS_MAPPING,
  LEAD_TIME_EXCLUDE_STAGE_FILTER,
  REPORT_LIST_METHOD
} from "../constant";
import { CHART_PROPS, COMPARE_FIELD, DEFAULT_QUERY, FILTERS, REPORT_NAME, URI } from "./constant";
import { LeadTimeSingleStatReportFiltersConfig } from "./filter.config";
import { prevQueryTransformer } from "./helper";

const leadTimeSingleStatReport: { azure_lead_time_single_stat: AzureLeadTimeStatReportType } = {
  azure_lead_time_single_stat: {
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
    [FILTER_NAME_MAPPING]: COMMON_FILTER_OPTIONS_MAPPING,
    [FILTER_WITH_INFO_MAPPING]: [LEAD_TIME_EXCLUDE_STAGE_FILTER],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: leadTimeSingleStatAzureSupportedFilters,
    drilldown: {},
    transformFunction: azureLeadTimeStatReportTransformer,
    chart_click_enable: false,
    [PREV_REPORT_TRANSFORMER]: prevQueryTransformer,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: LeadTimeSingleStatReportFiltersConfig
  }
};

export default leadTimeSingleStatReport;
