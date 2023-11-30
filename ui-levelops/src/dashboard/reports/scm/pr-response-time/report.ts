import { SCMPRReportsTransformer } from "custom-hooks/helpers/seriesData.helper";
import { githubPRSDrilldown } from "dashboard/constants/drilldown.constants";
import { ALLOWED_WIDGET_DATA_SORTING, VALUE_SORT_KEY } from "dashboard/constants/filter-name.mapping";
import { xAxisLabelTransform } from "dashboard/constants/helper";
import { githubPRsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMPrsResponseTimeReportType } from "model/report/scm/scm-prs-response-time-report/scmPrsResponseTimeReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { SCM_PRS_API_BASED_FILTERS } from "../pr-report/constant";
import { SCM_PRS_RESPONSE_TIME_REPORT_CHART_PROPS } from "./constant";
import { PrsResponseTimeReportFiltersConfig } from "./filter.config";
import { PR_RESPONSE_TIME_REPORT_QUERY, scmPrsResponseTimeReportChartProps } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmPrsResponseTimeReport: { github_prs_response_time_report: SCMPrsResponseTimeReportType } = {
  github_prs_response_time_report: {
    name: "SCM PRs Response Time Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.CIRCLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    convertTo: "days",
    filters_not_supporting_partial_filter: ["labels"],
    chart_props: SCM_PRS_RESPONSE_TIME_REPORT_CHART_PROPS,
    uri: "github_prs_author_response_time",
    reviewerUri: "github_prs_reviewer_response_time",
    method: "list",
    drilldown: githubPRSDrilldown,
    xAxisLabelTransform: xAxisLabelTransform,
    transformFunction: SCMPRReportsTransformer,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    onChartClickPayload: scmPrsResponseTimeReportChartProps,
    report_filters_config: PrsResponseTimeReportFiltersConfig,
    supported_filters: githubPRsSupportedFilters,
    API_BASED_FILTER: SCM_PRS_API_BASED_FILTERS,
    hide_custom_fields: true,
    weekStartsOnMonday: true,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "mean",
    default_query: PR_RESPONSE_TIME_REPORT_QUERY
  }
};
