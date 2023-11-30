import { ChartType } from "../../../../shared-resources/containers/chart-container/ChartType";
import { ChartContainerType, transformIssuesReportPrevQuery } from "../../../helpers/helper";
import {
  CHART_DATA_TRANSFORMERS,
  INFO_MESSAGES,
  MULTI_SERIES_REPORT_FILTERS_CONFIG,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG,
  STACKS_FILTER_STATUS
} from "../../../constants/applications/names";
import {
  ALLOW_KEY_FOR_STACKS,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  FILTER_KEY_MAPPING,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "../../../constants/filter-key.mapping";
import { issueManagementSupportedFilters } from "../../../constants/supported-filters.constant";
import { azureDrilldown } from "../../../constants/drilldown.constants";
import { azureTicketsReportChangeTransform } from "../../../../custom-hooks/helpers/helper";
import { FILTER_NAME_MAPPING, issueManagementCommonFilterOptionsMapping } from "../../../constants/filter-name.mapping";

import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_API_BASED_FILTERS,
  AZURE_DRILL_DOWN_VALUES_TO_FILTER_KEYS,
  REPORT_LIST_METHOD
} from "../constant";
import { AzureTicketsReportTypes } from "model/report/azure/tickets-report/tickets-report.model";
import {
  getDrillDownType,
  getTotalLabel,
  onChartClickPayload,
  shouldReverseAPIData,
  sortAPIDataHandler,
  stackFilterStatus
} from "./helper";
import {
  DEFAULT_QUERY,
  DEFAULT_ACROSS,
  INFORMATION_MESSAGE,
  APPEND_ACROSS_OPTIONS,
  CHART_PROPS,
  REPORT_NAME,
  STORY_POINT_URI,
  URI,
  FILTERS_KEY_MAPPING,
  ACROSS_OPTIONS,
  STACK_OPTIONS,
  CHART_DATA_TRANSFORMATION,
  METRIC_URI_MAPPING_LIST,
  METRIC_URI_MAPPING,
  ACROSS_FILTER_LABEL_MAPPING,
  FILTER_CONFIG_BASED_PREVIEW_FILTERS
} from "./constant";
import { IssuesReportFiltersConfig } from "./filter.config";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { AzureMultiSeriesTicketsReportFiltersConfig } from "dashboard/reports/multiseries-reports/azure/tickets-report/filter.config";
import { azureCommonFilterTransformFunc } from "../helpers/commonFilterTransform.helper";
import { generateBarColors } from "dashboard/reports/jira/issues-report/helper";
import { MAX_STACK_ENTRIES } from "dashboard/reports/jira/issues-report/constants";

const issuesReport: { azure_tickets_report: AzureTicketsReportTypes } = {
  azure_tickets_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.BAR,
    defaultAcross: DEFAULT_ACROSS,
    across: ACROSS_OPTIONS.map((item: { label: string; value: string }) => item.value),
    appendAcrossOptions: APPEND_ACROSS_OPTIONS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    stack_filters: STACK_OPTIONS.map((item: { label: string; value: string }) => item.value),
    maxStackEntries: MAX_STACK_ENTRIES,
    chart_props: CHART_PROPS,
    uri: URI,
    storyPointUri: STORY_POINT_URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    default_query: DEFAULT_QUERY,
    [STACKS_FILTER_STATUS]: stackFilterStatus,
    [INFO_MESSAGES]: INFORMATION_MESSAGE,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [ALLOW_KEY_FOR_STACKS]: true,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    [HIDE_REPORT]: true,
    sortApiDataHandler: sortAPIDataHandler,
    xAxisLabelTransform: getXAxisLabel,
    onChartClickPayload: onChartClickPayload,
    shouldReverseApiData: shouldReverseAPIData,
    transformFunction: azureTicketsReportChangeTransform,
    weekStartsOnMonday: true,
    widget_filter_transform: azureCommonFilterTransformFunc,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    valuesToFilters: AZURE_DRILL_DOWN_VALUES_TO_FILTER_KEYS,
    [FILTER_KEY_MAPPING]: FILTERS_KEY_MAPPING,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    getTotalLabel: getTotalLabel,
    filter_config_based_preview_filters: FILTER_CONFIG_BASED_PREVIEW_FILTERS,
    [PREV_REPORT_TRANSFORMER]: transformIssuesReportPrevQuery,
    [CHART_DATA_TRANSFORMERS]: CHART_DATA_TRANSFORMATION,
    [REPORT_FILTERS_CONFIG]: IssuesReportFiltersConfig,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: AzureMultiSeriesTicketsReportFiltersConfig,
    [METRIC_URI_MAPPING]: METRIC_URI_MAPPING_LIST,
    onUnmountClearData: true,
    acrossFilterLabelMapping: ACROSS_FILTER_LABEL_MAPPING,
    getDrillDownType: getDrillDownType,
    generateBarColors: generateBarColors
  }
};

export default issuesReport;
