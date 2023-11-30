import { statReportTransformer } from "custom-hooks/helpers";
import { githubIssuesStatDrilldown } from "dashboard/constants/drilldown.constants";
import { statDefaultQuery } from "dashboard/constants/helper";
import { githubIssuesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMIssuesSingleStatReportType } from "model/report/scm/scm-issues-single-stat/scmIssuesSingleStat.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { SCM_ISSUES_COUNT_SINGLE_STAT_CHART_PROPS, SCM_ISSUES_STAT_API_BASED_FILTERS } from "./constant";
import { IssuesCountSingleStatFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmIssuesCountSingleStatReport: { github_issues_count_single_stat: SCMIssuesSingleStatReportType } = {
  github_issues_count_single_stat: {
    name: "SCM Issues Count Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "scm_issues_report",
    method: "list",
    xaxis: true,
    defaultAcross: "issue_created",
    filters: {},
    chart_props: SCM_ISSUES_COUNT_SINGLE_STAT_CHART_PROPS,
    default_query: statDefaultQuery,
    compareField: "count",
    drilldown: githubIssuesStatDrilldown,
    transformFunction: statReportTransformer,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    supported_filters: githubIssuesSupportedFilters,
    API_BASED_FILTER: SCM_ISSUES_STAT_API_BASED_FILTERS,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    report_filters_config: IssuesCountSingleStatFiltersConfig,
    hide_custom_fields: true
  }
};
