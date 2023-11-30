import { scmCommitsStatReportTransformer } from "custom-hooks/helpers/scm-commits.helper";
import { githubCommitsStatDrilldown } from "dashboard/constants/drilldown.constants";
import { statDefaultQuery } from "dashboard/constants/helper";
import { githubCommitsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ScmCommitsSingleStatReportType } from "model/report/scm/scm-commits-single-stat/scmCommitsSingleStat.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_COMMITS_API_BASED_FILTERS, SCM_COMMIT_DEFAULT_QUERY } from "../commits/constant";
import { REPORT_FILTERS, SCM_COMMITS_SINGLE_STAT_CHART_PROPS } from "./constant";
import { CommitsSingleStatFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmCommitsStatReport: { github_commits_single_stat: ScmCommitsSingleStatReportType } = {
  github_commits_single_stat: {
    name: "SCM Commits Single Stat",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    show_max: false,
    xaxis: false,
    chart_props: SCM_COMMITS_SINGLE_STAT_CHART_PROPS,
    filters: REPORT_FILTERS,
    uri: "github_commits_report",
    method: "list",
    default_query: { ...statDefaultQuery, ...SCM_COMMIT_DEFAULT_QUERY },
    compareField: "count",
    drilldown: githubCommitsStatDrilldown,
    API_BASED_FILTER: SCM_COMMITS_API_BASED_FILTERS,
    supported_filters: githubCommitsSupportedFilters,
    transformFunction: scmCommitsStatReportTransformer,
    chart_click_enable: false,
    report_filters_config: CommitsSingleStatFiltersConfig,
    hide_custom_fields: true
  }
};
