import { tableTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { jiraAssigneeTimeDrilldown } from "dashboard/constants/drilldown.constants";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { IssueAssigneeTimeReportType } from "model/report/jira/issues-assignee-time-report/issueAssigneeTimeReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { issueAssigneeTimeHiddenFilters, issueFirstAssigneeChartProps } from "./constants";
import { JiraIssuesAssigneeTimeReportFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const issueAssigneeTimeReport: { assignee_time_report: IssueAssigneeTimeReportType } = {
  assignee_time_report: {
    name: "Issue Assignee Time Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: issueFirstAssigneeChartProps,
    uri: "assignee_time_report",
    method: "list",
    filters: {},
    hidden_filters: issueAssigneeTimeHiddenFilters,
    supported_filters: jiraSupportedFilters,
    drilldown: jiraAssigneeTimeDrilldown,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    transformFunction: data => tableTransformer(data),
    [REPORT_FILTERS_CONFIG]: JiraIssuesAssigneeTimeReportFiltersConfig,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default issueAssigneeTimeReport;
