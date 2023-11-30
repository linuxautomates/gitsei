import { SCMReportsTransformer } from "custom-hooks/helpers/seriesData.helper";
import { githubIssuesDrilldown } from "dashboard/constants/drilldown.constants";
import { githubIssuesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMIssuesReportType } from "model/report/scm/scm-issues-report/scmIssuesReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import {
  SCM_ISSUES_API_BASED_FILTERS,
  SCM_ISSUES_FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS,
  SCM_ISSUE_REPORT_CHART_PROPS
} from "./constant";
import { IssuesReportFiltersConfig } from "./filter.config";
import { scmIssuesReportChartClickPayloadHelper } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmIssuesReport: { github_issues_report: SCMIssuesReportType } = {
  github_issues_report: {
    name: "SCM Issues Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    chart_props: SCM_ISSUE_REPORT_CHART_PROPS,
    uri: "scm_issues_report",
    method: "list",
    filters_not_supporting_partial_filter: SCM_ISSUES_FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS,
    drilldown: githubIssuesDrilldown,
    transformFunction: SCMReportsTransformer,
    API_BASED_FILTER: SCM_ISSUES_API_BASED_FILTERS,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    supported_filters: githubIssuesSupportedFilters,
    report_filters_config: IssuesReportFiltersConfig,
    hide_custom_fields: true,
    onChartClickPayload: scmIssuesReportChartClickPayloadHelper
  }
};
