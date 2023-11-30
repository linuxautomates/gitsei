import { tableTransformer } from "custom-hooks/helpers";
import { scmReposReportDrilldown } from "dashboard/constants/drilldown.constants";
import { githubCommitsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMReposReportType } from "model/report/scm/scm-repos-report/scmReposReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { REPOS_FILTERS, SCM_REPOS_API_BASED_FILTERS, SCM_REPOS_REPORT_CHART_PROPS } from "./constant";
import { ReposReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmReposReport: { scm_repos_report: SCMReposReportType } = {
  scm_repos_report: {
    name: "SCM Repos Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    filters_not_supporting_partial_filter: ["repo_ids", "projects"],
    filters: REPOS_FILTERS,
    chart_props: SCM_REPOS_REPORT_CHART_PROPS,
    uri: "scm_repos",
    method: "list",
    drilldown: scmReposReportDrilldown,
    transformFunction: tableTransformer,
    chart_click_enable: true,
    API_BASED_FILTER: SCM_REPOS_API_BASED_FILTERS,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    supported_filters: githubCommitsSupportedFilters,
    report_filters_config: ReposReportFiltersConfig,
    hide_custom_fields: true
  }
};
