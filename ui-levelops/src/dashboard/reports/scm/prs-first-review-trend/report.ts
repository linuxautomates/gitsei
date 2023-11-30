import { trendReportTransformer } from "custom-hooks/helpers";
import { githubPRSFirstReviewTrendsDrilldown } from "dashboard/constants/drilldown.constants";
import { githubPRsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMPrsFirstReviewTrendReportType } from "model/report/scm/scm-prs-first-review-trend-report/scmPrsFirstReviewTrend.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { SCM_PRS_API_BASED_FILTERS } from "../pr-report/constant";
import { SCM_PRS_FIRST_REVIEW_TREND_CHART_PROPS, SCM_PRS_FIRST_REVIEW_TREND_COMPOSITE_TRANSFORM } from "./constant";
import { PrsFirstReviewTrendsFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmPrsFirstReviewTrendReport: {
  github_prs_first_review_trends: SCMPrsFirstReviewTrendReportType;
} = {
  github_prs_first_review_trends: {
    name: "SCM PRs First Review Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    filters_not_supporting_partial_filter: ["labels"],
    xaxis: true,
    transform: "median",
    composite: true,
    composite_transform: SCM_PRS_FIRST_REVIEW_TREND_COMPOSITE_TRANSFORM,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    defaultAcross: "pr_created",
    chart_props: SCM_PRS_FIRST_REVIEW_TREND_CHART_PROPS,
    uri: "scm_prs_first_review_trend",
    method: "list",
    convertTo: "hours",
    transformFunction: trendReportTransformer,
    drilldown: githubPRSFirstReviewTrendsDrilldown,
    supported_filters: githubPRsSupportedFilters,
    API_BASED_FILTER: SCM_PRS_API_BASED_FILTERS,
    report_filters_config: PrsFirstReviewTrendsFiltersConfig,
    hide_custom_fields: true
  }
};
