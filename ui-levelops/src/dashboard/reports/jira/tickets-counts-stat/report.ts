import { issuesSingleStatReportTransformer } from "custom-hooks/helpers/issuesSingleStat.helper";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { jiraStatDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  DEFAULT_METADATA,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY,
  SHOW_SETTINGS_TAB,
  STAT_TIME_BASED_FILTER
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformIssuesSingleStatReportPrevQuery } from "dashboard/helpers/helper";
import { issuesSingleStatValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { TicketsCountsStatTypes } from "model/report/jira/tickets-Counts-Stat/ticketsCountsStatReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { issueSingleStatDefaultMeta, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { REPORT_FILTERS_CONFIG } from "../../../constants/applications/names";
import {
  jiraTicketsCountsStatReportChartTypes,
  jiraTicketsCountsStatReportDefaultQuery,
  jiraTicketsCountsStatReportStatTimeBasedFilters
} from "./constants";
import { JiraIssuesSingleStatFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { mapFiltersBeforeCallIssueSingleStat } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const ticketsCountsStat: { tickets_counts_stat: TicketsCountsStatTypes } = {
  tickets_counts_stat: {
    name: "Issues Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "tickets_report",
    method: "list",
    filters: {},
    xaxis: false,
    chart_props: jiraTicketsCountsStatReportChartTypes,
    defaultAcross: "issue_created",
    default_query: jiraTicketsCountsStatReportDefaultQuery,
    compareField: "total_tickets",
    supported_filters: jiraSupportedFilters,
    drilldown: jiraStatDrilldown,
    transformFunction: (data: any) => issuesSingleStatReportTransformer(data),
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [SHOW_SETTINGS_TAB]: true,
    [WIDGET_VALIDATION_FUNCTION]: issuesSingleStatValidationHelper,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformIssuesSingleStatReportPrevQuery(data),
    [DEFAULT_METADATA]: issueSingleStatDefaultMeta,
    [STAT_TIME_BASED_FILTER]: jiraTicketsCountsStatReportStatTimeBasedFilters,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssuesSingleStatFiltersConfig,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    mapFiltersForWidgetApi: mapFiltersBeforeCallIssueSingleStat,
  }
};
export default ticketsCountsStat;
