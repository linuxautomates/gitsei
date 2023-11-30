import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { azureStatDrilldown } from "dashboard/constants/drilldown.constants";
import {
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  HIDE_REPORT,
  DEFAULT_METADATA,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS
} from "dashboard/constants/filter-key.mapping";
import {
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping,
  WIDGET_VALIDATION_FUNCTION
} from "dashboard/constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformAzureIssuesSingleStatReportPrevQuery } from "dashboard/helpers/helper";
import { issuesSingleStatValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { issueSingleStatDefaultMeta } from "dashboard/reports/jira/commonJiraReports.constants";
import { AzureTicketsCountSingleStat } from "model/report/azure/ticket-count-stat/ticket-count-stat-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_API_BASED_FILTERS,
  REPORT_LIST_METHOD
} from "../constant";
import {
  CHART_PROPS,
  COMPARE_FIELD,
  DEFAULT_ACROSS,
  DEFAULT_QUERY,
  METRIC_URI_MAPPING,
  METRIC_URI_MAPPING_LIST,
  REPORT_NAME,
  SUPPORTED_WIDGET_TYPES,
  URI
} from "./constant";
import { transformFunction } from "./helper";
import { IssueSingleStatFiltersConfig } from "./filter.config";
import { FILTER_CONFIG_BASED_PREVIEW_FILTERS, STORY_POINT_URI } from "../issues-report/constant";
import { getTotalLabel } from "../issues-report/helper";
import { azureCommonFilterTransformFunc } from "../helpers/commonFilterTransform.helper";

const issuesSingleStatReport: { azure_tickets_counts_stat: AzureTicketsCountSingleStat } = {
  azure_tickets_counts_stat: {
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
    storyPointUri: STORY_POINT_URI,
    default_query: DEFAULT_QUERY,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    compareField: COMPARE_FIELD,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureStatDrilldown,
    transformFunction: transformFunction,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    supported_widget_types: SUPPORTED_WIDGET_TYPES,
    chart_click_enable: false,
    [HIDE_REPORT]: true,
    widget_filter_transform: azureCommonFilterTransformFunc,
    filter_config_based_preview_filters: FILTER_CONFIG_BASED_PREVIEW_FILTERS,
    [WIDGET_VALIDATION_FUNCTION]: issuesSingleStatValidationHelper,
    [DEFAULT_METADATA]: issueSingleStatDefaultMeta,
    [PREV_REPORT_TRANSFORMER]: transformAzureIssuesSingleStatReportPrevQuery,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: IssueSingleStatFiltersConfig,
    getTotalLabel: getTotalLabel,
    [METRIC_URI_MAPPING]: METRIC_URI_MAPPING_LIST,
    onUnmountClearData: true
  }
};

export default issuesSingleStatReport;
