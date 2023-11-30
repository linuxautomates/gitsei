import { statReportTransformer } from "custom-hooks/helpers";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { githubCommitsStatDrilldown } from "dashboard/constants/drilldown.constants";
import { githubCommitsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMCodingDaysSingleStatReportType } from "model/report/scm/scm-coding-days-single-stat/scmCodingDaysSingleStat.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { REPORT_FILTERS, SCM_CODING_DAYS_STAT_CHART_PROPS, SCM_CODING_DAYS_STAT_DEFAULT_QUERY } from "./constant";
import { CodingDaysSingleStatReportFiltersConfig } from "./filter.config";
import { prevQueryTransformer, widgetValidationFunction } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmCodingDaysSingleStat: { github_coding_days_single_stat: SCMCodingDaysSingleStatReportType } = {
  github_coding_days_single_stat: {
    name: "SCM Coding Days Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: false,
    filters: REPORT_FILTERS,
    chart_props: SCM_CODING_DAYS_STAT_CHART_PROPS,
    uri: "github_coding_day",
    method: "list",
    default_query: SCM_CODING_DAYS_STAT_DEFAULT_QUERY,
    compareField: "mean",
    drilldown: githubCommitsStatDrilldown,
    transformFunction: statReportTransformer,
    supported_widget_types: ["stats"],
    chart_click_enable: false,
    report_filters_config: CodingDaysSingleStatReportFiltersConfig,
    API_BASED_FILTER: ["authors", "committers"],
    supported_filters: githubCommitsSupportedFilters,
    hide_custom_fields: true,
    widget_validation_function: widgetValidationFunction,
    show_in_days: true,
    [PREV_REPORT_TRANSFORMER]: prevQueryTransformer
  }
};
