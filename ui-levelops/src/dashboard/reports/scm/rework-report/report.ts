import { scmReworkReportTransformer } from "custom-hooks/helpers/seriesData.helper";
import { scmReworkReportDrilldown } from "dashboard/constants/drilldown.constants";
import { xAxisLabelTransform } from "dashboard/constants/helper";
import { githubCommitsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformSCMPrevQuery } from "dashboard/helpers/helper";
import { SCMReworkReportType } from "model/report/scm/rework-report/scmReworkReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { SCM_REWORK_API_BASED_FILTERS, SCM_REWORK_DESCRIPTION, SCM_REWORK_REPORT_CHART_PROPS } from "./constant";
import { SCMReworkReportFiltersConfig } from "./filters.config";
import { scmReworkOnChartClickPayloadHelper } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmReworkReport: { scm_rework_report: SCMReworkReportType } = {
  scm_rework_report: {
    name: "SCM Rework Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    description: SCM_REWORK_DESCRIPTION,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "author",
    chart_props: SCM_REWORK_REPORT_CHART_PROPS,
    uri: "scm_rework_report",
    method: "list",
    drilldown: scmReworkReportDrilldown,
    transformFunction: scmReworkReportTransformer,
    xAxisLabelTransform: xAxisLabelTransform,
    onChartClickPayload: scmReworkOnChartClickPayloadHelper,
    API_BASED_FILTER: SCM_REWORK_API_BASED_FILTERS,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    report_filters_config: SCMReworkReportFiltersConfig,
    supported_filters: githubCommitsSupportedFilters,
    prev_report_transformer: transformSCMPrevQuery
  }
};
