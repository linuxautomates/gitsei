import {
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG,
  TRANSFORM_LEGEND_LABEL
} from "../../../constants/applications/names";
import { sprintMetricTrendReportDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformSprintMetricsTrendReportPrevQuery } from "dashboard/helpers/helper";
import { SprintMetricTrendReportType } from "model/report/jira/sprint-metric-trend-report/sprintMetricTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { sprintMetricsTrendTransformer } from "transformers/reports/sprintMetricsPercentReportTransformer";
import {
  sprintMetricsTrendReportDefaultQuery,
  sprintMetricTrendChartProps,
  sprintMetricTrendReportColumnInfo
} from "./constants";
import {
  IGNORE_FILTER_KEYS_CONFIG,
  jiraSprintIgnoreConfig,
  BAR_CHART_REF_LINE_STROKE,
  DEFAULT_METADATA,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  PARTIAL_FILTER_MAPPING_KEY,
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  REQUIRED_ONE_FILTER,
  REQUIRED_ONE_FILTER_KEYS
} from "dashboard/constants/filter-key.mapping";
import { sprintDefaultMeta, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { JiraSprintMetricTrendReportFiltersConfig } from "./filters.config";
import {
  JIRA_PARTIAL_FILTER_KEY_MAPPING,
  requiredOneFiltersKeys,
  WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS,
  WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY
} from "../constant";
import { handleRequiredForFilters } from "../commonJiraReports.helper";
import { legendLabelTransform } from "dashboard/reports/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const sprintMetricTrendReport: { sprint_metrics_trend: SprintMetricTrendReportType } = {
  sprint_metrics_trend: {
    name: "Sprint Metrics Trend Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "jira_sprint_report",
    method: "list",
    filters: {
      include_issue_keys: true
    },
    xaxis: true,
    defaultAcross: "sprint",
    show_max: false,
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    columnWithInformation: true,
    columnsWithInfo: sprintMetricTrendReportColumnInfo,
    chart_props: sprintMetricTrendChartProps,
    default_query: sprintMetricsTrendReportDefaultQuery,
    compareField: "delivered_story_points",
    supported_filters: jiraSupportedFilters,
    drilldown: sprintMetricTrendReportDrilldown,
    transformFunction: data => sprintMetricsTrendTransformer(data),
    [BAR_CHART_REF_LINE_STROKE]: "#4f4f4f",
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraSprintMetricTrendReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformSprintMetricsTrendReportPrevQuery(data),
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS,
    [REQUIRED_ONE_FILTER]: (config: any, query: any, report: string) => handleRequiredForFilters(config, query, report),
    [REQUIRED_ONE_FILTER_KEYS]: requiredOneFiltersKeys,
    [TRANSFORM_LEGEND_LABEL]: legendLabelTransform
  }
};

export default sprintMetricTrendReport;
