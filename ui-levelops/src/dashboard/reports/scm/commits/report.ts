import { scmCommitsReportTransformer } from "custom-hooks/helpers/scm-commits.helper";
import { CHART_TOOLTIP_RENDER_TRANSFORM } from "dashboard/constants/applications/names";
import { githubCommitsChartTooltipTransformer } from "dashboard/constants/chartTooltipTransform/github-commitsChartTooltipTransformer";
import { scmCommitsReportDrilldown } from "dashboard/constants/drilldown.constants";
import { xAxisLabelTransform } from "dashboard/constants/helper";
import { githubCommitsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformSCMPrevQuery } from "dashboard/helpers/helper";
import { ScmCommitsReportType } from "model/report/scm/scm-commits-report/scmCommitsReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_DRILLDOWN_VALUES_TO_FILTER, SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import {
  SCM_COMMITS_API_BASED_FILTERS,
  SCM_COMMITS_BY_REPO_DESCRIPTION,
  SCM_COMMITS_REPORT_CHART_PROPS,
  SCM_COMMIT_DEFAULT_QUERY
} from "./constant";
import { CommitsReportFiltersConfig } from "./filter.config";
import { scmCommitsReportOnChartClickPayloadHelper, scmWidgetChartPropsHelper } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const SCMCommitsReport: { github_commits_report: ScmCommitsReportType } = {
  github_commits_report: {
    name: "SCM Commits Report",
    application: IntegrationTypes.GITHUB,
    description: SCM_COMMITS_BY_REPO_DESCRIPTION,
    chart_type: ChartType?.CIRCLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    chart_props: SCM_COMMITS_REPORT_CHART_PROPS,
    uri: "github_commits_report",
    method: "list",
    drilldown: scmCommitsReportDrilldown,
    onChartClickPayload: scmCommitsReportOnChartClickPayloadHelper,
    transformFunction: scmCommitsReportTransformer,
    xAxisLabelTransform: xAxisLabelTransform,
    default_query: SCM_COMMIT_DEFAULT_QUERY,
    get_widget_chart_props: scmWidgetChartPropsHelper,
    CHART_DATA_TRANSFORMERS: {
      [CHART_TOOLTIP_RENDER_TRANSFORM]: githubCommitsChartTooltipTransformer
    },
    API_BASED_FILTER: SCM_COMMITS_API_BASED_FILTERS,
    supported_filters: githubCommitsSupportedFilters,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    report_filters_config: CommitsReportFiltersConfig,
    hide_custom_fields: true,
    prev_report_transformer: transformSCMPrevQuery,
    valuesToFilters: SCM_DRILLDOWN_VALUES_TO_FILTER
  }
};
