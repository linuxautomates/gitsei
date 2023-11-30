import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { azureSprintMetricTrendReportDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
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
import { handleRequiredForFilters } from "dashboard/reports/jira/commonJiraReports.helper";
import { requiredOneFiltersKeys } from "dashboard/reports/jira/constant";
import { AzureSprintPercentageTrendReportType } from "model/report/azure/sprint-metrics-percentage-trend-report/sprint-metrics-percentage-trend-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { sprintMetricsPercentReportTransformer } from "transformers/reports/sprintMetricsPercentReportTransformer";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  COMMON_FILTER_OPTIONS_MAPPING,
  REPORT_LIST_METHOD
} from "../constant";
import { CHART_PROPS, COLUMNS_WITH_INFO, DEFAULT_ACROSS, DEFAULT_QUERY, REPORT_NAME, URI } from "./constant";
import { SprintMetricPercentageTrendReportFiltersConfig } from "./filter.config";

const sprintMetricsPercentageTrendReport: {
  azure_sprint_metrics_percentage_trend: AzureSprintPercentageTrendReportType;
} = {
  azure_sprint_metrics_percentage_trend: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.AREA,
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
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureSprintMetricTrendReportDrilldown,
    transformFunction: sprintMetricsPercentReportTransformer,
    [HIDE_REPORT]: true,
    [FILTER_NAME_MAPPING]: COMMON_FILTER_OPTIONS_MAPPING,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: SprintMetricPercentageTrendReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer,
    [REQUIRED_ONE_FILTER]: (config: any, query: any, report: string) => handleRequiredForFilters(config, query, report),
    [REQUIRED_ONE_FILTER_KEYS]: requiredOneFiltersKeys
  }
};

export default sprintMetricsPercentageTrendReport;
