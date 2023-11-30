import { tableTransformer } from "custom-hooks/helpers";
import { scmCommittersReportDrilldown } from "dashboard/constants/drilldown.constants";
import { githubCommitsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMCommittersReportType } from "model/report/scm/scm-committer-report/scmCommittersReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "../constant";
import { REPORTS_FILTERS, SCM_COMMITTERS_API_BASED_FILTERS, SCM_COMMITTERS_REPORT_CHART_PROPS } from "./constant";
import { CommittersReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmCommittersReport: { scm_committers_report: SCMCommittersReportType } = {
  scm_committers_report: {
    name: "SCM Committers Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    filters_not_supporting_partial_filter: ["repo_ids", "projects"],
    xaxis: false,
    chart_props: SCM_COMMITTERS_REPORT_CHART_PROPS,
    uri: "scm_committers",
    method: "list",
    filters: REPORTS_FILTERS,
    drilldown: scmCommittersReportDrilldown,
    transformFunction: tableTransformer,
    supported_filters: githubCommitsSupportedFilters,
    FIELD_KEY_FOR_FILTERS: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING,
    API_BASED_FILTER: SCM_COMMITTERS_API_BASED_FILTERS,
    report_filters_config: CommittersReportFiltersConfig,
    hide_custom_fields: true
  }
};
