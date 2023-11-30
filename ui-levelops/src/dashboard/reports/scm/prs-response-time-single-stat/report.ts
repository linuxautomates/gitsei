import { statReportTransformer } from "custom-hooks/helpers";
import { githubPRSStatDrilldown } from "dashboard/constants/drilldown.constants";
import { statDefaultQuery } from "dashboard/constants/helper";
import { githubPRsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMPrsResponseTimeSingleStatReportType } from "model/report/scm/scm-prs-response-time-single-stat/scmPrsResponseTimeSingleStat.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_PRS_API_BASED_FILTERS } from "../pr-report/constant";
import { SCM_PRS_RESPONSE_TIME_STAT_CHART_PROPS } from "./constant";
import { PrsResponseTimeSingleStatFiltersConfig } from "./filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmPrsResponseTimeSingleStat: {
  github_prs_response_time_single_stat: SCMPrsResponseTimeSingleStatReportType;
} = {
  github_prs_response_time_single_stat: {
    name: "SCM PRs Response Time Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: true,
    defaultAcross: "pr_created",
    chart_props: SCM_PRS_RESPONSE_TIME_STAT_CHART_PROPS,
    uri: "github_prs_author_response_time",
    reviewerUri: "github_prs_reviewer_response_time",
    method: "list",
    filters: {},
    default_query: statDefaultQuery,
    compareField: "mean",
    report_filters_config: PrsResponseTimeSingleStatFiltersConfig,
    supported_filters: githubPRsSupportedFilters,
    API_BASED_FILTER: SCM_PRS_API_BASED_FILTERS,
    drilldown: githubPRSStatDrilldown,
    transformFunction: statReportTransformer,
    supported_widget_types: ["stats"],
    chart_click_enable: false
  }
};
