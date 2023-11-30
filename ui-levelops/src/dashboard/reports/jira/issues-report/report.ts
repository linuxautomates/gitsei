import { jiraTicketsReportChangeTransform } from "custom-hooks/helpers/helper";
import {
  MULTI_SERIES_REPORT_FILTERS_CONFIG,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformIssuesReportPrevQuery } from "dashboard/helpers/helper";
import { JiraIssuesReportType } from "model/report/jira/issues-report/issuesReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  JIRA_TICKET_REPORT_CHART_PROPS,
  JIRA_TICKET_REPORT_INFO_MESSAGE,
  JIRA_TICKET_REPORT_FILTER_OPTION_MAP,
  JIRA_TICKET_REPORT_QUERY,
  ADD_EXTRA_FILTER,
  MAX_STACK_ENTRIES
} from "./constants";
import {
  addExtraFilters,
  generateBarColors,
  getTotalLabelIssuesReport,
  issuesReportGetStackFilterStatus,
  issuesReportXAxisLabelTransform,
  mapFiltersBeforeCallIssueReport,
  transformFinalFilters
} from "./helper";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  VALUE_SORT_KEY
} from "dashboard/constants/filter-name.mapping";
import {
  ALLOW_KEY_FOR_STACKS,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { jiraOnChartClickPayload } from "../commonJiraReports.helper";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { JiraIssuesReportFiltersConfig } from "./filters.config";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { JiraIssueProgressReport } from "dashboard/report-filters/jira/issue-progress-report-filter.config";
import { JiraMultiSeriesIssuesReportFiltersConfig } from "dashboard/reports/multiseries-reports/jira/tickets-report/filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const issuesReport: { tickets_report: JiraIssuesReportType } = {
  tickets_report: {
    name: "Issues Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    defaultAcross: "assignee",
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    chart_props: JIRA_TICKET_REPORT_CHART_PROPS,
    uri: "tickets_report",
    storyPointUri: "story_point_report",
    method: "list",
    filters: {},
    default_query: JIRA_TICKET_REPORT_QUERY,
    stack_filters: [...jiraSupportedFilters.values, "ticket_category"],
    maxStackEntries: MAX_STACK_ENTRIES,
    [ALLOW_KEY_FOR_STACKS]: true,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraDrilldown,
    weekStartsOnMonday: true,
    infoMessages: JIRA_TICKET_REPORT_INFO_MESSAGE,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "ticket_count",
    [REPORT_FILTERS_CONFIG]: JiraIssuesReportFiltersConfig,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: JiraMultiSeriesIssuesReportFiltersConfig,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [FILTER_NAME_MAPPING]: JIRA_TICKET_REPORT_FILTER_OPTION_MAP,
    getStacksStatus: issuesReportGetStackFilterStatus,
    xAxisLabelTransform: issuesReportXAxisLabelTransform,
    onChartClickPayload: jiraOnChartClickPayload,
    transformFunction: data => jiraTicketsReportChangeTransform(data),
    getTotalLabel: getTotalLabelIssuesReport,
    [PREV_REPORT_TRANSFORMER]: data => transformIssuesReportPrevQuery(data),
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [ADD_EXTRA_FILTER]: (query: any, type: string, value: string, customEpics: any) =>
      addExtraFilters(query, type, value, customEpics),
    mapFiltersBeforeCall: mapFiltersBeforeCallIssueReport,
    generateBarColors: generateBarColors,
    transformFinalFilters: transformFinalFilters
  }
};

export default issuesReport;
