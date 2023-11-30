import { trendReportTransformer } from "custom-hooks/helpers";
import { githubIssuesDrilldown } from "dashboard/constants/drilldown.constants";
import { githubIssuesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { scmIssueTrendTransformer } from "dashboard/helpers/drilldown-transformers/githubDrilldownTransformer";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMIssuesTrendReportType } from "model/report/scm/scm-issues-trend-report/scmIssuesTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { SCM_ISSUES_REPORT_TRENDS_CHART_PROPS, SCM_ISSUES_TREND_API_BASED_FILTERS } from "./constant";
import { IssuesReportTrendsFiltersConfig } from "./filter.config";
import { scmIssuesTrendReportChartClickPayload } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmIssuesReportTrends: { github_issues_report_trends: SCMIssuesTrendReportType } = {
  github_issues_report_trends: {
    name: "SCM Issues Report Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    filters_not_supporting_partial_filter: ["labels"],
    transform: "count",
    composite: true,
    defaultAcross: "issue_created",
    chart_props: SCM_ISSUES_REPORT_TRENDS_CHART_PROPS,
    uri: "scm_issues_report",
    method: "list",
    drilldown: {
      ...githubIssuesDrilldown,
      drilldownTransformFunction: scmIssueTrendTransformer
    },
    transformFunction: trendReportTransformer,
    API_BASED_FILTER: SCM_ISSUES_TREND_API_BASED_FILTERS,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    onChartClickPayload: scmIssuesTrendReportChartClickPayload,
    report_filters_config: IssuesReportTrendsFiltersConfig,
    supported_filters: githubIssuesSupportedFilters,
    hide_custom_fields: true
  }
};
