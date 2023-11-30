import { jiraBacklogTransformerWrapper } from "custom-hooks/helpers";
import {
  FE_BASED_FILTERS,
  MULTI_SERIES_REPORT_FILTERS_CONFIG,
  PREV_REPORT_TRANSFORMER
} from "dashboard/constants/applications/names";
import { jiraBacklogDrillDown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "dashboard/constants/filter-key.mapping";
import { ALLOWED_WIDGET_DATA_SORTING, FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformIssueBacklogTrendReportPrevQuery } from "dashboard/helpers/helper";
import { JiraBacklogTrendReportTypes } from "model/report/jira/jira-backlog-trend-report/jiraBacklogTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { issue_resolved_at, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { REPORT_FILTERS_CONFIG } from "./../../../constants/applications/names";
import { jiraBacklogTrendDefaultQuery, jiraBacklogTrendReportChartTypes } from "./constants";
import { JiraBacklogTrendReportFiltersConfig } from "./filters.config";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { get } from "lodash";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { JiraMultiSeriesBacklogTrendReportFiltersConfig } from "dashboard/reports/multiseries-reports/jira/backlog-trend-report/filter.config";
import { customBacklogChartProps, getBacklogChartUnits } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraBacklogTrendReport: { jira_backlog_trend_report: JiraBacklogTrendReportTypes } = {
  jira_backlog_trend_report: {
    name: "Issue Backlog Trend Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: jiraBacklogTrendReportChartTypes,
    uri: "backlog_report",
    method: "list",
    filters: {
      across: "trend"
    },
    stack_filters: jiraSupportedFilters.values,
    default_query: jiraBacklogTrendDefaultQuery,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraBacklogDrillDown,
    // shouldReverseApiData: () => true,
    transformFunction: data => jiraBacklogTransformerWrapper(data),
    get_custom_chart_props: customBacklogChartProps,
    getChartUnits: getBacklogChartUnits,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    xAxisLabelTransform: getXAxisLabel,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: data => transformIssueBacklogTrendReportPrevQuery(data),
    [REPORT_FILTERS_CONFIG]: JiraBacklogTrendReportFiltersConfig,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: JiraMultiSeriesBacklogTrendReportFiltersConfig,
    onChartClickPayload: (param: any) => {
      const timeStamp = get(param, ["data", "activePayload", 0, "payload", "key"], undefined);
      const label = get(param, ["data", "activeLabel"], undefined);
      return { id: timeStamp, name: label };
    },
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default jiraBacklogTrendReport;
