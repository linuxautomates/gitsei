import { SCMReviewCollaborationTransformer } from "custom-hooks/helpers";
import { getGraphFilters } from "custom-hooks/helpers/scm-prs.helper";
import { githubReviewCollaborationDrilldown } from "dashboard/constants/drilldown.constants";
import { API_BASED_FILTER } from "dashboard/constants/filter-key.mapping";
import { githubPRsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMReviewCollaborationReportType } from "model/report/scm/scm-review-collboration-report/scmReviewCollaborationReport";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { SCM_REVIEW_COLLABORATION_DESCRIPTION } from "../scm-review-collaboration/constant";
import { SCMReviewCollabReportFiltersConfig } from "../scm-review-collaboration/filters.config";
import {
  SCM_COLLABORATION_API_BASED_FILTERS,
  SCM_COLLABORATION_DEFAULT_QUERY,
  SCM_REVIEW_COLLABORATION_CHART_PROPS
} from "./constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmReviewCollaborationReport: { review_collaboration_report: SCMReviewCollaborationReportType } = {
  review_collaboration_report: {
    name: "SCM Review Collaboration Report",
    application: IntegrationTypes.GITHUB,
    description: SCM_REVIEW_COLLABORATION_DESCRIPTION,
    chart_type: ChartType?.REVIEW_SCM_SANKEY,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: SCM_REVIEW_COLLABORATION_CHART_PROPS,
    uri: "scm_review_collaboration_report",
    method: "list",
    filters: {},
    default_query: SCM_COLLABORATION_DEFAULT_QUERY,
    widget_height: "36rem",
    drilldown: githubReviewCollaborationDrilldown,
    report_filters_config: SCMReviewCollabReportFiltersConfig,
    supported_filters: githubPRsSupportedFilters,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    transformFunction: SCMReviewCollaborationTransformer,
    shouldFocusOnDrilldown: true,
    API_BASED_FILTER: SCM_COLLABORATION_API_BASED_FILTERS,
    filters_not_supporting_partial_filter: ["labels"],
    includeMissingFieldsInPreview: true,
    get_graph_filters: getGraphFilters,
    onUnmountClearData: true
  }
};
