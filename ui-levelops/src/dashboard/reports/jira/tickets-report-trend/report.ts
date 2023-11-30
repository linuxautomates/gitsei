import { REPORT_FILTERS_CONFIG } from "../../../constants/applications/names";
import { trendReportTransformer } from "custom-hooks/helpers";
import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY,
  SHOW_SETTINGS_TAB
} from "dashboard/constants/filter-key.mapping";
import { ALLOWED_WIDGET_DATA_SORTING, FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { TicketReportTrendTypes } from "model/report/jira/tickets-report-trend/ticketsReportTrend.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import {
  jiraTicketsReportTrendChartTypes,
  jiraTicketsReportTrendDefaultQuery,
  jiraTicketsReportTrendFilters
} from "./constants";
import { JiraIssuesTrendReportTrendsFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const ticketsReportTrend: { tickets_report_trends: TicketReportTrendTypes } = {
  tickets_report_trends: {
    name: "Issues Trend Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      total_tickets: "total_jira_tickets"
    },
    chart_props: jiraTicketsReportTrendChartTypes,
    uri: "tickets_report",
    method: "list",
    filters: jiraTicketsReportTrendFilters,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [SHOW_SETTINGS_TAB]: true,
    default_query: jiraTicketsReportTrendDefaultQuery,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraDrilldown,
    transformFunction: (data: any) => trendReportTransformer(data),
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [SHOW_SETTINGS_TAB]: true,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssuesTrendReportTrendsFiltersConfig,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default ticketsReportTrend;
