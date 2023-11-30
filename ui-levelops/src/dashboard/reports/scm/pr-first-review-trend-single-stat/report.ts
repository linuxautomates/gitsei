import { statReportTransformer } from "custom-hooks/helpers";
import { githubPRSStatDrilldown } from "dashboard/constants/drilldown.constants";
import { statDefaultQuery } from "dashboard/constants/helper";
import { githubPRsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMPrsFirstReviewSingleStatReportType } from "model/report/scm/scm-prs-first-review-trend-single-stat/scmPrsFirstReviewTrendSingleStat.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_PRS_API_BASED_FILTERS } from "../pr-report/constant";
import { PrsFirstReviewTrendSingleStatFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmPrsFirstReviewTrendSingleStat: {
  github_prs_first_review_single_stat: SCMPrsFirstReviewSingleStatReportType;
} = {
  github_prs_first_review_single_stat: {
    name: "SCM PRs First Review Trend Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: true,
    defaultAcross: "pr_created",
    chart_props: {},
    uri: "scm_prs_first_review_trend",
    method: "list",
    filters: {},
    default_query: statDefaultQuery,
    compareField: "sum",
    drilldown: githubPRSStatDrilldown,
    transformFunction: statReportTransformer,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    supported_filters: githubPRsSupportedFilters,
    API_BASED_FILTER: SCM_PRS_API_BASED_FILTERS,
    report_filters_config: PrsFirstReviewTrendSingleStatFiltersConfig,
    hide_custom_fields: true
  }
};
