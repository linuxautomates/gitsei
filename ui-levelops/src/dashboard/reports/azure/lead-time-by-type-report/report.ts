import { leadTimeTypeTransformer } from "custom-hooks/helpers";
import {
  PREVIEW_DISABLED,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
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
import { AzureLeadTimeByTypeReportType } from "model/report/azure/lead-time-by-type/lead-time-by-type.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  COMMON_FILTER_OPTIONS_MAPPING,
  LEAD_TIME_EXCLUDE_STAGE_FILTER,
  REPORT_LIST_METHOD
} from "../constant";
import { CHART_PROPS, DEFAULT_ACROSS, DEFAULT_QUERY, FILTERS, REPORT_NAME, URI } from "./constant";
import { LeadTimeByTypeReportFiltersConfig } from "./filter.config";

const leadTimeByTypeReport: { azure_lead_time_by_type_report: AzureLeadTimeByTypeReportType } = {
  azure_lead_time_by_type_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.LEAD_TIME_TYPE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    // xaxis: true,  TODO: Add later, Out of scope for initial release
    chart_props: CHART_PROPS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    defaultAcross: DEFAULT_ACROSS,
    default_query: DEFAULT_QUERY,
    supported_filters: azureLeadTimeSupportedFilters,
    [FILTER_NAME_MAPPING]: COMMON_FILTER_OPTIONS_MAPPING,
    [FILTER_WITH_INFO_MAPPING]: [LEAD_TIME_EXCLUDE_STAGE_FILTER],
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PREVIEW_DISABLED]: true,
    drilldown: azureLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: leadTimeTypeTransformer,
    [HIDE_REPORT]: true,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer,
    shouldJsonParseXAxis: () => true,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: LeadTimeByTypeReportFiltersConfig
  }
};

export default leadTimeByTypeReport;
