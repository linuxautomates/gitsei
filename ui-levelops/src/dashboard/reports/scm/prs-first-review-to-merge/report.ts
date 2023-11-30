import { trendReportTransformer } from "custom-hooks/helpers";
import { githubPRSFirstReviewToMergeTrendsDrilldown } from "dashboard/constants/drilldown.constants";
import { githubPRsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMPrsFirstReviewMergeTrendReportType } from "model/report/scm/scm-first-review-to-merge-trend/scmFirstReviewToMergeTrend.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { SCM_PRS_API_BASED_FILTERS } from "../pr-report/constant";
import {
  SCM_PRS_FIRST_REVIEW_TO_MERGE_TREND_CHART_PROPS,
  SCM_PRS_FIRST_REVIEW_TO_MERGE_TREND_COMPOSITE_TRANSFORM
} from "./constant";
import { PrsFirstReviewToMergeTrendsFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmFirstReviewToMergeTrend: {
  github_prs_first_review_to_merge_trends: SCMPrsFirstReviewMergeTrendReportType;
} = {
  github_prs_first_review_to_merge_trends: {
    name: "SCM PRs First Review To Merge Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    transform: "median",
    composite: true,
    filters_not_supporting_partial_filter: ["labels"],
    composite_transform: SCM_PRS_FIRST_REVIEW_TO_MERGE_TREND_COMPOSITE_TRANSFORM,
    chart_props: SCM_PRS_FIRST_REVIEW_TO_MERGE_TREND_CHART_PROPS,
    uri: "scm_prs_first_review_to_merge_trend",
    method: "list",
    supported_filters: githubPRsSupportedFilters,
    API_BASED_FILTER: SCM_PRS_API_BASED_FILTERS,
    convertTo: "hours",
    transformFunction: trendReportTransformer,
    drilldown: githubPRSFirstReviewToMergeTrendsDrilldown,
    report_filters_config: PrsFirstReviewToMergeTrendsFiltersConfig,
    hide_custom_fields: true
  }
};
