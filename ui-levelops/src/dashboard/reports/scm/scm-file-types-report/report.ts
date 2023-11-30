import { tableTransformer } from "custom-hooks/helpers";
import { scmFilesTypesReportDrilldown } from "dashboard/constants/drilldown.constants";
import { githubCommitsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMFilesTypesReportType } from "model/report/scm/scm-files-types-report/scmFilesTypesReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { REPORT_FILTERS, SCM_FILES_TYPES_REPORT_CHART_PROPS, SCM_FILE_TYPES_API_BASED_FILTERS } from "./constant";
import { GithubFileTypeReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmFilesTypesReport: { scm_file_types_report: SCMFilesTypesReportType } = {
  scm_file_types_report: {
    name: "SCM File Types Report",
    application: IntegrationTypes.GITHUB,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: SCM_FILES_TYPES_REPORT_CHART_PROPS,
    uri: "scm_file_types",
    method: "list",
    filters: REPORT_FILTERS,
    filters_not_supporting_partial_filter: ["file_types"],
    drilldown: scmFilesTypesReportDrilldown,
    transformFunction: tableTransformer,
    API_BASED_FILTER: SCM_FILE_TYPES_API_BASED_FILTERS,
    supported_filters: githubCommitsSupportedFilters,
    report_filters_config: GithubFileTypeReportFiltersConfig,
    hide_custom_fields: true
  }
};
