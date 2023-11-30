import { trendReportTransformer } from "custom-hooks/helpers";
import { githubPRSDrilldown } from "dashboard/constants/drilldown.constants";
import { githubPRsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ScmPrsMergeTrendReportType } from "model/report/scm/scm-prs-merge-trend/scmPrsMergeTrendReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { SCM_PRS_API_BASED_FILTERS } from "../pr-report/constant";
import { SCM_PRS_MERGE_TREND_CHART_PROPS, SCM_PRS_MERGE_TREND_COMPOSITE_TRANSFORM } from "./constant";
import { PrsMergeTrendsFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmPrsMergeTrendReport: { github_prs_merge_trends: ScmPrsMergeTrendReportType } = {
  github_prs_merge_trends: {
    name: "SCM PRs Merge Trends",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    transform: "median",
    composite: true,
    composite_transform: SCM_PRS_MERGE_TREND_COMPOSITE_TRANSFORM,
    defaultAcross: "pr_created",
    chart_props: SCM_PRS_MERGE_TREND_CHART_PROPS,
    uri: "scm_prs_merge_trend",
    method: "list",
    filters_not_supporting_partial_filter: ["labels"],
    convertTo: "hours",
    drilldown: githubPRSDrilldown,
    supported_filters: githubPRsSupportedFilters,
    API_BASED_FILTER: SCM_PRS_API_BASED_FILTERS,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    transformFunction: trendReportTransformer,
    report_filters_config: PrsMergeTrendsFiltersConfig,
    hide_custom_fields: true
  }
};
