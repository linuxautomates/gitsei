import { trendReportTransformer } from "custom-hooks/helpers";
import { githubIssuesDrilldown } from "dashboard/constants/drilldown.constants";
import { githubIssuesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMIssuesFirstResponseTrendReportType } from "model/report/scm/scm-issues-first-response-trend-report/scmIssuesFirstResponseTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { SCM_FIRST_RESPONSE_TREND_API_BASED_FILTERS, SCM_FIRST_RESPONSE_TREND_CHART_PROPS } from "./constant";
import { IssuesFirstResponseTrendReportFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmIssuesFirstResponseTrendReport: {
  github_issues_first_response_report_trends: SCMIssuesFirstResponseTrendReportType;
} = {
  github_issues_first_response_report_trends: {
    name: "SCM Issues First Response Report Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    transform: "median",
    composite: false,
    defaultAcross: "issue_created",
    chart_props: SCM_FIRST_RESPONSE_TREND_CHART_PROPS,
    filters_not_supporting_partial_filter: ["labels"],
    uri: "scm_issues_first_response_report",
    method: "list",
    convertTo: "days",
    drilldown: githubIssuesDrilldown,
    transformFunction: trendReportTransformer,
    IS_FRONTEND_REPORT: true,
    API_BASED_FILTER: SCM_FIRST_RESPONSE_TREND_API_BASED_FILTERS,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    supported_filters: githubIssuesSupportedFilters,
    report_filters_config: IssuesFirstResponseTrendReportFiltersConfig
  }
};
