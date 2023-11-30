import { SCMPRReportsTransformer } from "custom-hooks/helpers/seriesData.helper";
import { githubPRSDrilldown } from "dashboard/constants/drilldown.constants";
import { xAxisLabelTransform } from "dashboard/constants/helper";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ScmPrsReportType } from "model/report/scm/scm-prs-report/scmPrsReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  SCM_PRS_API_BASED_FILTERS,
  SCM_PRS_CHART_PROPS,
  SCM_PRS_FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS,
  SCM_PRS_REPORT_DESCRIPTION
} from "./constant";
import { PrsReportFiltersConfig } from "./filter.config";
import { PR_REPORT_QUERY, scmPrsReportChartClickPayload, transformSCMPRsReportPrevQuery } from "./helper";
import { ALLOWED_WIDGET_DATA_SORTING, VALUE_SORT_KEY } from "dashboard/constants/filter-name.mapping";
import { SCM_DRILLDOWN_VALUES_TO_FILTER, SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { githubPRsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { DEFAULT_METADATA, ALLOW_KEY_FOR_STACKS } from "dashboard/constants/filter-key.mapping";
import { SCMVisualizationTypes } from "dashboard/constants/typeConstants";
import { generateBarColors } from "dashboard/reports/jira/issues-report/helper";

export const SCMPrsReport: { github_prs_report: ScmPrsReportType } = {
  github_prs_report: {
    name: "SCM PRs Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.BAR,
    description: SCM_PRS_REPORT_DESCRIPTION,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "repo_id",
    chart_props: SCM_PRS_CHART_PROPS,
    uri: "github_prs_report",
    method: "list",
    weekStartsOnMonday: true,
    [ALLOW_KEY_FOR_STACKS]: true,
    supported_filters: githubPRsSupportedFilters,
    filters_not_supporting_partial_filter: SCM_PRS_FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS,
    drilldown: githubPRSDrilldown,
    xAxisLabelTransform: xAxisLabelTransform,
    onChartClickPayload: scmPrsReportChartClickPayload,
    transformFunction: SCMPRReportsTransformer,
    report_filters_config: PrsReportFiltersConfig,
    API_BASED_FILTER: SCM_PRS_API_BASED_FILTERS,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    hide_custom_fields: true,
    valuesToFilters: SCM_DRILLDOWN_VALUES_TO_FILTER,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [PREV_REPORT_TRANSFORMER]: transformSCMPRsReportPrevQuery,
    [VALUE_SORT_KEY]: "count",
    default_query: PR_REPORT_QUERY,
    [DEFAULT_METADATA]: {
      visualization: SCMVisualizationTypes.BAR_CHART
    },
    generateBarColors: generateBarColors
  }
};
