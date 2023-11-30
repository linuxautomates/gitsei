import { scmIssueFirstResponseReport } from "custom-hooks/helpers";
import { githubIssuesDrilldown } from "dashboard/constants/drilldown.constants";
import { githubIssuesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMIssuesFirstResponseReportType } from "model/report/scm/scm-issues-first-response-report/scmIssuesFirstResponseReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { SCM_ISSUES_FIRST_RESPONSE_API_BASED_FILTERS, SCM_ISSUES_FIRST_RESPONSE_REPORT_CHART_PROPS } from "./constant";
import { IssuesFirstResponseReportFiltersConfig } from "./filter.config";
import { scmIssuesFirstResponseReportOnChartClickHelper } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmIssuesFirstResponseReport: { github_issues_first_reponse_report: SCMIssuesFirstResponseReportType } = {
  github_issues_first_reponse_report: {
    name: "SCM Issues First Response Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    filters_not_supporting_partial_filter: ["labels"],
    chart_props: SCM_ISSUES_FIRST_RESPONSE_REPORT_CHART_PROPS,
    uri: "scm_issues_first_response_report",
    method: "list",
    drilldown: githubIssuesDrilldown,
    transformFunction: scmIssueFirstResponseReport,
    API_BASED_FILTER: SCM_ISSUES_FIRST_RESPONSE_API_BASED_FILTERS,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    supported_filters: githubIssuesSupportedFilters,
    onChartClickPayload: scmIssuesFirstResponseReportOnChartClickHelper,
    report_filters_config: IssuesFirstResponseReportFiltersConfig,
    hide_custom_fields: true
  }
};
