import { scmFilesTransform } from "custom-hooks/helpers";
import { scmFilesReportDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMFilesReportType } from "model/report/scm/scm-files-report/scmFilesReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { REPORT_FILTERS, SCM_FILES_REPORT_DESCRIPTION } from "./constants";
import { FilesReportFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmFilesReport: { scm_files_report: SCMFilesReportType } = {
  scm_files_report: {
    name: "SCM Files Report",
    application: IntegrationTypes.GITHUB,
    description: SCM_FILES_REPORT_DESCRIPTION,
    chart_type: ChartType?.GRID_VIEW,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    uri: "scm_files_report",
    filters: REPORT_FILTERS,
    rootFolderURI: "scm_files_root_folder_report",
    method: "list",
    drilldown: scmFilesReportDrilldown,
    transformFunction: scmFilesTransform,
    report_filters_config: FilesReportFiltersConfig,
    hide_custom_fields: true
  }
};
