import { statReportTransformer } from "custom-hooks/helpers";
import { githubIssuesStatDrilldown } from "dashboard/constants/drilldown.constants";
import { statDefaultQuery } from "dashboard/constants/helper";
import { githubIssuesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMIssuesFirstResponseSingleStatReportType } from "model/report/scm/scm-issues-first-response-single-stat/scmIssuesFirstResponseCountSingleStat.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { ISSUES_FIRST_RESPONSE_STAT_API_BASED_FILTERS, SCM_FIRST_RESPONSE_STAT_CHART_PROPS } from "./constant";
import { IssuesFirstResponseSingleStatFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmIssuesFirstResponseSingleStatReport: {
  github_issues_first_response_count_single_stat: SCMIssuesFirstResponseSingleStatReportType;
} = {
  github_issues_first_response_count_single_stat: {
    name: "SCM Issues First Response Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "scm_issues_first_response_report",
    method: "list",
    chart_props: SCM_FIRST_RESPONSE_STAT_CHART_PROPS,
    default_query: statDefaultQuery,
    compareField: "sum",
    drilldown: githubIssuesStatDrilldown,
    transformFunction: statReportTransformer,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    supported_filters: githubIssuesSupportedFilters,
    API_BASED_FILTER: ISSUES_FIRST_RESPONSE_STAT_API_BASED_FILTERS,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    report_filters_config: IssuesFirstResponseSingleStatFiltersConfig,
    hide_custom_fields: true
  }
};
