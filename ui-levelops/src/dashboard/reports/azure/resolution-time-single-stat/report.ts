import { azureIssueResolutionTimeReportStatTransformer } from "custom-hooks/helpers/issuesSingleStat.helper";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { azureStatDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  DEFAULT_METADATA,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import {
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping
} from "dashboard/constants/filter-name.mapping";
import { azureExcludeStatusFilter, FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import {
  ChartContainerType,
  transformAzureIssuesResolutionTimeSingleStatReportPrevQuery
} from "dashboard/helpers/helper";
import { AzureResolutionTimeSingleStatReportType } from "model/report/azure/resolution-time-single-stat/resolution-time-single-stat.model";
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
  DEFAULT_ACROSS,
  DEFAULT_QUERY,
  REPORT_NAME,
  SUPPORTED_WIDGET_TYPES,
  URI
} from "./constants";
import { ResolutionTimeSingleStatReportFiltersConfig } from "./filter.config";

const resolutionTimeSingleStatReport: {
  azure_resolution_time_counts_stat: AzureResolutionTimeSingleStatReportType;
} = {
  azure_resolution_time_counts_stat: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    xaxis: false,
    chart_props: CHART_PROPS,
    defaultAcross: DEFAULT_ACROSS,
    default_query: DEFAULT_QUERY,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    compareField: COMPARE_FIELD,
    supported_filters: issueManagementSupportedFilters,
    transformFunction: azureIssueResolutionTimeReportStatTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [azureExcludeStatusFilter],
    supported_widget_types: SUPPORTED_WIDGET_TYPES,
    [HIDE_REPORT]: true,
    [DEFAULT_METADATA]: DEFAULT_METADATA,
    drilldown: azureStatDrilldown,
    [PREV_REPORT_TRANSFORMER]: transformAzureIssuesResolutionTimeSingleStatReportPrevQuery,
    hasStatUnit: (compareField: string) => true,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: ResolutionTimeSingleStatReportFiltersConfig
  }
};

export default resolutionTimeSingleStatReport;
