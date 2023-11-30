import { jiraIssueResolutionTimeReportStatTransformer } from "custom-hooks/helpers/issuesSingleStat.helper";
import { jiraStatDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  DEFAULT_METADATA,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformIssueResolutionTimeSingleStatReportPrevQuery } from "dashboard/helpers/helper";
import { ResolutionTimeSingleStatType } from "model/report/jira/resolution-time-count-stat/resolutionTimeCountStat.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { FILTER_WITH_INFO_MAPPING, jiraExcludeStatusFilter } from "../../../constants/filterWithInfo.mapping";
import { issueSingleStatDefaultMeta, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { REPORT_FILTERS_CONFIG } from "./../../../constants/applications/names";
import { jiraSingleStatDefaultCreatedAt } from "./constants";
import { JiraResolutionTimeSingleStatFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const resolutionTimeCountStat: { resolution_time_counts_stat: ResolutionTimeSingleStatType } = {
  resolution_time_counts_stat: {
    name: "Issue Resolution Time Single Stat",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "resolution_time_report",
    method: "list",
    filters: {},
    xaxis: false,
    chart_props: {
      unit: "Days"
    },
    defaultAcross: "issue_created",
    default_query: { agg_type: "average", issue_created_at: jiraSingleStatDefaultCreatedAt },
    compareField: "median",
    supported_filters: jiraSupportedFilters,
    drilldown: jiraStatDrilldown,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [FILTER_WITH_INFO_MAPPING]: [jiraExcludeStatusFilter],
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    transformFunction: data => jiraIssueResolutionTimeReportStatTransformer(data),
    prev_report_transformer: data => transformIssueResolutionTimeSingleStatReportPrevQuery(data),
    [DEFAULT_METADATA]: issueSingleStatDefaultMeta,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraResolutionTimeSingleStatFiltersConfig,
    hasStatUnit: (compareField: string) => true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default resolutionTimeCountStat;
