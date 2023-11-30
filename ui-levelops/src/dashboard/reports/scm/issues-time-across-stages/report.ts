import { scmaResolutionTimeDataTransformer } from "custom-hooks/helpers/seriesData.helper";
import { githubTimeAcrossStagesDefaultQuery } from "dashboard/constants/applications/github.application";
import { scmIssueTimeAcrossStagesDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMIssuesTimeAcrossStagesReportType } from "model/report/scm/scm-issues-time-across-stages/scmIssuesTimeAcrossStages.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_TIME_ACROSS_STAGES_REPORT } from "./constant";
import { SCMIssuesTimeAcrossStagesFiltersConfig } from "./filters.config";
import { getTotalKeyHelper } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmIssuesTimeAcrossStages: {
  scm_issues_time_across_stages_report: SCMIssuesTimeAcrossStagesReportType;
} = {
  scm_issues_time_across_stages_report: {
    name: "SCM Issues Time Across Stages",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "column",
    default_query: githubTimeAcrossStagesDefaultQuery,
    xaxis: true,
    chart_props: SCM_ISSUES_TIME_ACROSS_STAGES_REPORT,
    uri: "scm_issues_time_across_stages",
    method: "list",
    filters: {},
    dataKey: "median_time",
    drilldown: scmIssueTimeAcrossStagesDrilldown,
    transformFunction: scmaResolutionTimeDataTransformer,
    report_filters_config: SCMIssuesTimeAcrossStagesFiltersConfig,
    weekStartsOnMonday: true,
    getTotalKey: getTotalKeyHelper
  }
};
