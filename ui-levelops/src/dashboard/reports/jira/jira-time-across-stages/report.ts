import { timeAcrossStagesDataTransformer } from "custom-hooks/helpers";
import {
  IMPLICITY_INCLUDE_DRILLDOWN_FILTER,
  TIME_FILTER_RANGE_CHOICE_MAPPER
} from "dashboard/constants/applications/names";
import { jiraDrilldown, jiraIssueTimeAcrossStagesDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY,
  SHOW_SETTINGS_TAB
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, jiraTimeAcrossFilterOptionsMapping } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING, jiraHideStatusFilter } from "dashboard/constants/filterWithInfo.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { JiraTimeAcrossStagesReportTypes } from "model/report/jira/jira_time_across_stages/jiraTimeAcrossStagesReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { REPORT_FILTERS_CONFIG } from "./../../../constants/applications/names";
import {
  jiraAcrossStagesChartTypes,
  jiraAcrossStagesDefaultQuery,
  jiraAcrossStagesIncludeSolveTimeImplicitFilter
} from "./constants";
import { JiraTimeAcrossStagesReportFiltersConfig } from "./filters.config";
import { xAxisLabelTransform } from "./helpers";
import { jiraOnChartClickPayload } from "../commonJiraReports.helper";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraTimeAcrossStagesReport: {
  jira_time_across_stages: JiraTimeAcrossStagesReportTypes;
} = {
  jira_time_across_stages: {
    name: "Issue Time Across Stages",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "none",
    default_query: jiraAcrossStagesDefaultQuery,
    chart_props: jiraAcrossStagesChartTypes,
    uri: "jira_time_across_stages_report",
    method: "list",
    filters: {},
    dataKey: "median_time",
    supported_filters: jiraSupportedFilters,
    drilldown: jiraIssueTimeAcrossStagesDrilldown,
    transformFunction: (data: any) => timeAcrossStagesDataTransformer(data),
    [FILTER_NAME_MAPPING]: jiraTimeAcrossFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [jiraHideStatusFilter],
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      issue_resolved_at: "jira_issue_resolved_at"
    },
    [SHOW_SETTINGS_TAB]: true,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraTimeAcrossStagesReportFiltersConfig,
    xAxisLabelTransform: xAxisLabelTransform,
    onChartClickPayload: jiraOnChartClickPayload,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: jiraAcrossStagesIncludeSolveTimeImplicitFilter,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};
export default jiraTimeAcrossStagesReport;
