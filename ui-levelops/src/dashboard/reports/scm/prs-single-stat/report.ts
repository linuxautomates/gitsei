import { statReportTransformer } from "custom-hooks/helpers";
import { githubPRSStatDrilldown } from "dashboard/constants/drilldown.constants";
import { statDefaultQuery } from "dashboard/constants/helper";
import { githubPRsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ScmPrsSingleStatReportType } from "model/report/scm/scm-prs-single-stat/scmPrsStatReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_PRS_API_BASED_FILTERS } from "../pr-report/constant";
import { SCM_PRS_STAT_CHART_PROPS } from "./constant";
import { PrsSingleStatFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmPRsSingleStatReport: { github_prs_single_stat: ScmPrsSingleStatReportType } = {
  github_prs_single_stat: {
    name: "SCM PRs Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    chart_props: SCM_PRS_STAT_CHART_PROPS,
    uri: "github_prs_report",
    method: "list",
    default_query: statDefaultQuery,
    defaultAcross: "pr_created",
    compareField: "count",
    drilldown: githubPRSStatDrilldown,
    transformFunction: statReportTransformer,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    supported_filters: githubPRsSupportedFilters,
    API_BASED_FILTER: SCM_PRS_API_BASED_FILTERS,
    report_filters_config: PrsSingleStatFiltersConfig,
    hide_custom_fields: true
  }
};
