import { scmFilesTransform } from "custom-hooks/helpers";
import { scmJiraFilesReportDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { SCMJiraFilesReportType } from "model/report/scm/scm-jira-files-report/scmJiraFilesReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ScmJiraFilesFiltersConfig } from "./filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const scmJiraFilesReport: { scm_jira_files_report: SCMJiraFilesReportType } = {
  scm_jira_files_report: {
    name: "Issue Hotspots Report",
    application: IntegrationTypes.GITHUBJIRA,
    chart_type: ChartType?.GRID_VIEW,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    uri: "scm_jira_files_report",
    rootFolderURI: "scm_jira_files_root_folder_report",
    method: "list",
    drilldown: scmJiraFilesReportDrilldown,
    transformFunction: scmFilesTransform,
    chart_click_enable: true,
    report_filters_config: ScmJiraFilesFiltersConfig,
    hide_custom_fields: true
  }
};
