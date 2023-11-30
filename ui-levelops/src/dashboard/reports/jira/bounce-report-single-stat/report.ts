import { statReportTransformer } from "custom-hooks/helpers";
import { jiraStatDrilldown } from "dashboard/constants/drilldown.constants";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { JiraBounceSingleStatReportType } from "model/report/jira/bounce-report-single-stat/bounceReportSingleStat.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { FE_BASED_FILTERS } from "../../../constants/applications/names";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { issue_resolved_at, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { REPORT_FILTERS_CONFIG } from "./../../../constants/applications/names";
import { bounceSingleStatChartProps, bounceSingleStatDefaultQuery, bounceStatWidgetFilters } from "./contants";
import { JiraIssueBounceSingleStat } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraBounceSingleStatReport: { bounce_counts_stat: JiraBounceSingleStatReportType } = {
  bounce_counts_stat: {
    name: "Issue Bounce Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bounce_report",
    method: "list",
    filters: bounceStatWidgetFilters,
    xaxis: false,
    chart_props: bounceSingleStatChartProps,
    default_query: bounceSingleStatDefaultQuery,
    compareField: "median",
    supported_filters: jiraSupportedFilters,
    drilldown: jiraStatDrilldown,
    transformFunction: data => statReportTransformer(data),
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [FE_BASED_FILTERS]: {
      issue_resolved_at
    },
    [REPORT_FILTERS_CONFIG]: JiraIssueBounceSingleStat,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default jiraBounceSingleStatReport;
