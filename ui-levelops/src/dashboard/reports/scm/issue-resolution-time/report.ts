import { scmaResolutionTimeDataTransformer } from "custom-hooks/helpers/seriesData.helper";
import { githubresolutionTimeDefaultQuery } from "dashboard/constants/applications/github.application";
import { scmResolutionDrillDown } from "dashboard/constants/drilldown.constants";
import { API_BASED_FILTER } from "dashboard/constants/filter-key.mapping";
import { githubIssuesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { scmResolutionTimeTooltipMapping } from "dashboard/graph-filters/components/Constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMIssuesTimeResolutionReportType } from "model/report/scm/scm-issues-time-resolution-report/scmIssuesTimeResolutionReport.constant";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import {
  SCM_ISSUES_RESOLUTION_TIME_CHART_PROPS,
  SCM_RESOLUTION_TIME_API_BASED_FILTERS,
  SCM_RESOLUTION_TIME_DATA_KEYS
} from "./constant";
import { IssuesResolutionTimeReportFiltersConfig } from "./filter.config";
import { resolutionTimeGetGraphFilters } from "./getGraphFilters";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmIssuesResolutionTimeReport: { scm_issues_time_resolution_report: SCMIssuesTimeResolutionReportType } = {
  scm_issues_time_resolution_report: {
    name: "SCM Issues Resolution Time Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "issue_created",
    default_query: githubresolutionTimeDefaultQuery,
    xaxis: true,
    chart_props: SCM_ISSUES_RESOLUTION_TIME_CHART_PROPS,
    tooltipMapping: scmResolutionTimeTooltipMapping,
    uri: "scm_resolution_time_report",
    method: "list",
    dataKey: SCM_RESOLUTION_TIME_DATA_KEYS,
    drilldown: scmResolutionDrillDown,
    transformFunction: scmaResolutionTimeDataTransformer,
    weekStartsOnMonday: true,
    supported_filters: githubIssuesSupportedFilters,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    API_BASED_FILTER: SCM_RESOLUTION_TIME_API_BASED_FILTERS,
    xAxisLabelTransform: getXAxisLabel,
    get_graph_filters: resolutionTimeGetGraphFilters,
    report_filters_config: IssuesResolutionTimeReportFiltersConfig,
    hide_custom_fields: true
  }
};
