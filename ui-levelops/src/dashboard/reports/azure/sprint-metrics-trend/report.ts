import {
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG,
  TRANSFORM_LEGEND_LABEL
} from "dashboard/constants/applications/names";
import { azureSprintMetricTrendReportDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  BAR_CHART_REF_LINE_STROKE,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  IGNORE_FILTER_KEYS_CONFIG,
  jiraSprintIgnoreConfig,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  REQUIRED_ONE_FILTER,
  REQUIRED_ONE_FILTER_KEYS
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { legendLabelTransform } from "dashboard/reports/helper";
import { handleRequiredForFilters } from "dashboard/reports/jira/commonJiraReports.helper";
import { requiredOneFiltersKeys } from "dashboard/reports/jira/constant";
import { AzureSprintMetricsTrendReportType } from "model/report/azure/sprint-metrics-trend-report/sprint-metrics-trend-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { sprintMetricsTrendTransformer } from "transformers/reports/sprintMetricsPercentReportTransformer";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  COMMON_FILTER_OPTIONS_MAPPING,
  REPORT_LIST_METHOD
} from "../constant";
import {
  BAR_CHART_STROKE_COLOR,
  CHART_PROPS,
  COLUMNS_WITH_INFO,
  COMPARE_FIELD,
  DEFAULT_ACROSS,
  DEFAULT_QUERY,
  REPORT_NAME,
  URI
} from "./constant";
import { SprintMetricTrendReportFiltersConfig } from "./filter.config";

const sprintMetricsTrendReport: { azure_sprint_metrics_trend: AzureSprintMetricsTrendReportType } = {
  azure_sprint_metrics_trend: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {
      include_workitem_ids: true
    },
    xaxis: true,
    defaultAcross: DEFAULT_ACROSS,
    show_max: false,
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    columnWithInformation: true,
    columnsWithInfo: COLUMNS_WITH_INFO,
    chart_props: CHART_PROPS,
    default_query: DEFAULT_QUERY,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    compareField: COMPARE_FIELD,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureSprintMetricTrendReportDrilldown,
    transformFunction: sprintMetricsTrendTransformer,
    [HIDE_REPORT]: true,
    [BAR_CHART_REF_LINE_STROKE]: BAR_CHART_STROKE_COLOR,
    [FILTER_NAME_MAPPING]: COMMON_FILTER_OPTIONS_MAPPING,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: SprintMetricTrendReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer,
    [REQUIRED_ONE_FILTER]: (config: any, query: any, report: string) => handleRequiredForFilters(config, query, report),
    [REQUIRED_ONE_FILTER_KEYS]: requiredOneFiltersKeys,
    [TRANSFORM_LEGEND_LABEL]: legendLabelTransform
  }
};

export default sprintMetricsTrendReport;
