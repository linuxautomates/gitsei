import { REPORT_FILTERS_CONFIG } from "../../../constants/applications/names";
import { sprintStatReportTransformer } from "custom-hooks/helpers/sprintStatReporthelper";
import { sprintMetricSingleStatDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { sprintMetricStatCsvTransformer } from "dashboard/helpers/csv-transformers/sprintMetricStatCSVTransformer";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SprintMetricSingleStatType } from "model/report/jira/sprint-metric-single-stat/sprintMetricSingleStat.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { sprintMetricColumnsWithInfo, sprintMetricDefaultQuery } from "./constants";
import {
  CSV_DRILLDOWN_TRANSFORMER,
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
import { jiraApiBasedFilterKeyMapping, sprintDefaultMeta } from "../commonJiraReports.constants";
import { JiraSprintSingleStatReportFiltersConfig } from "./filters.config";
import {
  JIRA_PARTIAL_FILTER_KEY_MAPPING,
  requiredOneFiltersKeys,
  WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS,
  WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY
} from "../constant";
import { handleRequiredForFilters } from "../commonJiraReports.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const sprintMetricSingleStat: { sprint_metrics_single_stat: SprintMetricSingleStatType } = {
  sprint_metrics_single_stat: {
    name: "Sprint Metrics Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.SPRINT_API_WRAPPER,
    uri: "jira_sprint_report",
    method: "list",
    filters: {
      include_issue_keys: true
    },
    default_query: sprintMetricDefaultQuery,
    chart_props: {
      unit: "%"
    },
    xaxis: false,
    columnWithInformation: true,
    columnsWithInfo: sprintMetricColumnsWithInfo,
    supported_filters: jiraSupportedFilters,
    drilldown: {
      allowDrilldown: true,
      ...sprintMetricSingleStatDrilldown
    },
    supported_widget_types: ["stats"],
    [CSV_DRILLDOWN_TRANSFORMER]: sprintMetricStatCsvTransformer,
    transformFunction: data => sprintStatReportTransformer(data),
    [IGNORE_FILTER_KEYS_CONFIG]: jiraSprintIgnoreConfig,
    [DEFAULT_METADATA]: sprintDefaultMeta,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraSprintSingleStatReportFiltersConfig,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS,
    [REQUIRED_ONE_FILTER]: (config: any, query: any, report: string) => handleRequiredForFilters(config, query, report),
    [REQUIRED_ONE_FILTER_KEYS]: requiredOneFiltersKeys
  }
};

export default sprintMetricSingleStat;
