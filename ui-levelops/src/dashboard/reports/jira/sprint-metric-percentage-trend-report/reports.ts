import { REPORT_FILTERS_CONFIG } from "../../../constants/applications/names";
import { sprintMetricTrendReportDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SprintMetricPercentageTrendReportType } from "model/report/jira/sprint-metrics-percentage-trend-report/sprintMetricPercentageTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { sprintMetricsPercentReportTransformer } from "transformers/reports/sprintMetricsPercentReportTransformer";
import {
  sprintMetricPerTrendChartProps,
  sprintMetricPerTrendReportColumnsWithInfo,
  sprintMetricsPercentageReport
} from "./constants";
import { sprintMetricPerTrendReportChartClickPayload } from "./helper";
import {
  IGNORE_FILTER_KEYS_CONFIG,
  jiraSprintIgnoreConfig,
  DEFAULT_METADATA,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  PARTIAL_FILTER_MAPPING_KEY,
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  REQUIRED_ONE_FILTER,
  REQUIRED_ONE_FILTER_KEYS
} from "../../../constants/filter-key.mapping";
import { sprintDefaultMeta, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { JiraSprintMetricPercentageTrendReportFiltersConfig } from "./filters.config";
import {
  JIRA_PARTIAL_FILTER_KEY_MAPPING,
  requiredOneFiltersKeys,
  WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS,
  WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY
} from "../constant";
import { handleRequiredForFilters } from "../commonJiraReports.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const sprintMetricPercentageTrendReport: {
  sprint_metrics_percentage_trend: SprintMetricPercentageTrendReportType;
} = {
  sprint_metrics_percentage_trend: {
    name: "Sprint Metrics Percentage Trend Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jira_sprint_report",
    method: "list",
    filters: {
      include_issue_keys: true
    },
    xaxis: true,
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    defaultAcross: "bi_week",
    show_max: false,
    columnWithInformation: true,
    columnsWithInfo: sprintMetricPerTrendReportColumnsWithInfo,
    chart_props: sprintMetricPerTrendChartProps,
    default_query: sprintMetricsPercentageReport,
    supported_filters: jiraSupportedFilters,
    drilldown: sprintMetricTrendReportDrilldown,
    onChartClickPayload: sprintMetricPerTrendReportChartClickPayload,
    transformFunction: (data: basicMappingType<any>) => sprintMetricsPercentReportTransformer(data),
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraSprintMetricPercentageTrendReportFiltersConfig,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS,
    [REQUIRED_ONE_FILTER]: (config: any, query: any, report: string) => handleRequiredForFilters(config, query, report),
    [REQUIRED_ONE_FILTER_KEYS]: requiredOneFiltersKeys
  }
};

export default sprintMetricPercentageTrendReport;
