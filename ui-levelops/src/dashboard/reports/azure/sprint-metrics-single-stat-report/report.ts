import { sprintStatReportTransformer } from "custom-hooks/helpers/sprintStatReporthelper";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  API_BASED_FILTER,
  CSV_DRILLDOWN_TRANSFORMER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  IGNORE_FILTER_KEYS_CONFIG,
  jiraSprintIgnoreConfig,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  REQUIRED_ONE_FILTER,
  REQUIRED_ONE_FILTER_KEYS
} from "dashboard/constants/filter-key.mapping";
import {
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping
} from "dashboard/constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { sprintMetricStatCsvTransformer } from "dashboard/helpers/csv-transformers/sprintMetricStatCSVTransformer";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { handleRequiredForFilters } from "dashboard/reports/jira/commonJiraReports.helper";
import { requiredOneFiltersKeys } from "dashboard/reports/jira/constant";
import { AzureSprintMetricsSingleStatReportType } from "model/report/azure/sprint-metrics-single-stat/sprint-metrics-single-stat.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  REPORT_LIST_METHOD
} from "../constant";
import {
  CHART_PROPS,
  COLUMNS_WITH_INFO,
  DEFAULT_QUERY,
  DRILL_DOWN,
  FILTERS,
  REPORT_NAME,
  SUPPORTED_WIDGET_TYPES,
  URI
} from "./constant";
import { SprintMetricsSingleStatReportFiltersConfig } from "./filter.config";

const sprintMetricsSingleStatReport: { azure_sprint_metrics_single_stat: AzureSprintMetricsSingleStatReportType } = {
  azure_sprint_metrics_single_stat: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.SPRINT_API_WRAPPER,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    default_query: DEFAULT_QUERY,
    xaxis: false,
    chart_props: CHART_PROPS,
    columnWithInformation: true,
    columnsWithInfo: COLUMNS_WITH_INFO,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: DRILL_DOWN,
    [CSV_DRILLDOWN_TRANSFORMER]: sprintMetricStatCsvTransformer,
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    transformFunction: sprintStatReportTransformer,
    supported_widget_types: SUPPORTED_WIDGET_TYPES,
    [HIDE_REPORT]: true,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: SprintMetricsSingleStatReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer,
    [REQUIRED_ONE_FILTER]: (config: any, query: any, report: string) => handleRequiredForFilters(config, query, report),
    [REQUIRED_ONE_FILTER_KEYS]: requiredOneFiltersKeys
  }
};

export default sprintMetricsSingleStatReport;
