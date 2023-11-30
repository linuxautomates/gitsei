import { leadTimeTrendTransformer } from "custom-hooks/helpers";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { azureLeadTimeDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  CSV_DRILLDOWN_TRANSFORMER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { azureLeadTimeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { leadTimeCsvTransformer } from "dashboard/helpers/csv-transformers/leadTimeCsvTransformer";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { azureLeadTimeTrendPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureLeadTimeTrendReportPreviousQuery.transformer";
import { AzureLeadTimeTrendReportType } from "model/report/azure/lead-time-trend-report/lead-time-trend-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  COMMON_FILTER_OPTIONS_MAPPING,
  LEAD_TIME_EXCLUDE_STAGE_FILTER,
  REPORT_LIST_METHOD
} from "../constant";
import { CHART_PROPS, DEFAULT_QUERY, filters, REPORT_NAME, URI } from "./constant";
import { LeadTimeTrendReportFiltersConfig } from "./filter.config";

const leadTimeTrendReport: { azure_lead_time_trend_report: AzureLeadTimeTrendReportType } = {
  azure_lead_time_trend_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: CHART_PROPS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: filters,
    default_query: DEFAULT_QUERY,
    convertTo: "days",
    widget_height: "375px",
    [FILTER_NAME_MAPPING]: COMMON_FILTER_OPTIONS_MAPPING,
    [FILTER_WITH_INFO_MAPPING]: [LEAD_TIME_EXCLUDE_STAGE_FILTER],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: azureLeadTimeSupportedFilters,
    drilldown: azureLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: leadTimeTrendTransformer,
    [HIDE_REPORT]: true,
    shouldJsonParseXAxis: () => true,
    [PREV_REPORT_TRANSFORMER]: azureLeadTimeTrendPrevQueryTansformer,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: LeadTimeTrendReportFiltersConfig
  }
};

export default leadTimeTrendReport;
