import { timeAcrossStagesDataTransformer } from "custom-hooks/helpers";
import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { azureIssueTimeAcrossStagesDrilldown } from "dashboard/constants/drilldown.constants";
import {
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  HIDE_REPORT,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS
} from "dashboard/constants/filter-key.mapping";
import {
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping
} from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING, azureHideStatusFilter } from "dashboard/constants/filterWithInfo.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { AzureTimeAcrossStagesReportType } from "model/report/azure/time-across-stages/time-across-stages-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  drillDownValuesToFiltersKeys,
  ADO_APPLICATION,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_API_BASED_FILTERS,
  REPORT_LIST_METHOD
} from "../constant";
import { timeAcrossStagesOnChartClickPayload, timeAcrossStagesXAxisLabelTransform } from "./helper";
import { includeSolveTimeImplicitFilter } from "../../../constants/applications/constant";
import {
  APPEND_ACROSS_OPTIONS,
  azureAcrossStagesDefaultQuery,
  DATA_KEY,
  DEFAULT_ACROSS,
  ISSUE_TIME_ACROSS_BAR_PROPS,
  REPORT_NAME,
  TIME_ACROSS_STAGES_REPORT_OPTIONS,
  URI
} from "./constant";
import { IssueTimeAcrossFiltersConfig } from "./filter.config";
import { azureTimeAcrossStagesPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureIssueTimeAcrossStagesPrevQuery.transformer";

const issueTimeAcrossStagesReport: { azure_time_across_stages: AzureTimeAcrossStagesReportType } = {
  azure_time_across_stages: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: DEFAULT_ACROSS,
    across: TIME_ACROSS_STAGES_REPORT_OPTIONS,
    chart_props: ISSUE_TIME_ACROSS_BAR_PROPS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    dataKey: DATA_KEY,
    default_query: azureAcrossStagesDefaultQuery,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    appendAcrossOptions: APPEND_ACROSS_OPTIONS,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureIssueTimeAcrossStagesDrilldown,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [azureHideStatusFilter],
    [HIDE_REPORT]: true,
    valuesToFilters: drillDownValuesToFiltersKeys,
    prev_report_transformer: azureTimeAcrossStagesPrevQueryTansformer,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter,
    [REPORT_FILTERS_CONFIG]: IssueTimeAcrossFiltersConfig,
    xAxisLabelTransform: timeAcrossStagesXAxisLabelTransform,
    transformFunction: timeAcrossStagesDataTransformer,
    onChartClickPayload: timeAcrossStagesOnChartClickPayload
  }
};

export default issueTimeAcrossStagesReport;
