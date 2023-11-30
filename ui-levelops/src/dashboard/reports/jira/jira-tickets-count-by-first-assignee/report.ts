import { jiraTicketsReportChangeTransform } from "custom-hooks/helpers/helper";
import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  SHOW_SETTINGS_TAB
} from "dashboard/constants/filter-key.mapping";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  VALUE_SORT_KEY
} from "dashboard/constants/filter-name.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { JiraTicketsCountByFirstAssigneeReportTypes } from "model/report/jira/jira-tickets-count-by-first-assignee/jiraTicketsCountByFirstAssigneeReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { issue_resolved_at, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { jiraOnChartClickPayload } from "../commonJiraReports.helper";
import { REPORT_FILTERS_CONFIG } from "./../../../constants/applications/names";
import {
  jiraTicketsCountByFirstAssigneeReportChartTypes,
  jiraTicketsCountByFirstAssigneeReportDefaultQuery,
  jiraTicketsCountByFirstAssigneeReportFilter
} from "./constants";
import { JiraIssuesByFirstAssigneeFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraTicketsCountByFirstAssigneeReport: {
  jira_tickets_count_by_first_assignee: JiraTicketsCountByFirstAssigneeReportTypes;
} = {
  jira_tickets_count_by_first_assignee: {
    name: "Issues By First Assignee",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: jiraTicketsCountByFirstAssigneeReportChartTypes,
    uri: "tickets_report",
    method: "list",
    filters: jiraTicketsCountByFirstAssigneeReportFilter,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "ticket_count",
    [SHOW_SETTINGS_TAB]: true,
    default_query: jiraTicketsCountByFirstAssigneeReportDefaultQuery,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraDrilldown,
    transformFunction: data => jiraTicketsReportChangeTransform(data),
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    xAxisLabelTransform: params => {
      const { across, item = {} } = params;
      const { key, additional_key } = item;
      let newLabel = key;
      if (["first_assignee"].includes(across)) {
        newLabel = additional_key;
      }
      if (!newLabel) {
        newLabel = "UNRESOLVED";
      }
      return newLabel;
    },
    onChartClickPayload: jiraOnChartClickPayload,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssuesByFirstAssigneeFiltersConfig,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default jiraTicketsCountByFirstAssigneeReport;
