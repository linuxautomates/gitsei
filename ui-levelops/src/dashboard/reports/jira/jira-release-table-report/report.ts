import { jiraReleaseTableDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { JIRA_REVERSE_FILTER_KEY_MAPPING } from "../constant";
import {
  JIRA_RELEASE_TABLE_REPORT_DESCRIPTION,
  JIRA_RELEASE_TABLE_REPORT_API_BASED_FILTERS,
  JIRA_RELEASE_TABLE_REPORT_CHART_PROPS,
  JIRA_RELEASE_TABLE_REPORT_DEFAULT_QUERY,
  CUSTOM_FIELD_KEY
} from "./constant";
import { JiraReleaseTableReportFilterConfig } from "./filters.config";
import {
  JiraReleaseReportCSVColumn,
  JiraReleaseReportCSVDataTransformer,
  JiraReleaseReportCSVFiltersTransformer,
  dataTransformFunction,
  getJiraReleaseDrilldownTitle,
  getJiraReleaseDrilldownType,
  mapFiltersBeforeCall,
  onChartClickHandler
} from "./helper";
import { jiraReleaseTableReportTypes } from "model/report/jira/jira_release_table_report/jiraReleaseTableReport";
import { CATEGORY, GET_CUSTOMIZE_TITLE } from "dashboard/constants/filter-key.mapping";
import { REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { JiraReleaseTableConfig } from "dashboard/pages/dashboard-tickets/configs/jiraReleaseTableConfig";
import {
  REPORT_CSV_DOWNLOAD_CONFIG,
  WIDGET_MIN_HEIGHT
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { getJiraReleaseTitle } from "./jiraReleaseReportTitle";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraReleaseTableReport: {
  jira_release_table_report: jiraReleaseTableReportTypes;
} = {
  jira_release_table_report: {
    name: "Jira Releases Report",
    [CATEGORY]: "velocity",
    description: JIRA_RELEASE_TABLE_REPORT_DESCRIPTION,
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    default_query: JIRA_RELEASE_TABLE_REPORT_DEFAULT_QUERY,
    chart_props: JIRA_RELEASE_TABLE_REPORT_CHART_PROPS,
    uri: "jira_release_table_report",
    method: "list",
    drilldown: {
      ...jiraReleaseTableDrilldown,
      drilldownVisibleColumn: ["key", "summary", "velocity_stage_total_time", "issue_created_at", "priority"]
    },
    supported_filters: jiraSupportedFilters,
    FIELD_KEY_FOR_FILTERS: JIRA_REVERSE_FILTER_KEY_MAPPING,
    API_BASED_FILTER: JIRA_RELEASE_TABLE_REPORT_API_BASED_FILTERS,
    IS_FRONTEND_REPORT: true, // will remove this once report is available from BE
    [REPORT_FILTERS_CONFIG]: JiraReleaseTableReportFilterConfig,
    mapFiltersBeforeCall: mapFiltersBeforeCall,
    mapFiltersForWidgetApi: mapFiltersBeforeCall,
    widgetTableColumn: JiraReleaseTableConfig,
    transformFunction: dataTransformFunction,
    [REPORT_CSV_DOWNLOAD_CONFIG]: {
      widgetFiltersTransformer: JiraReleaseReportCSVFiltersTransformer,
      widgetCSVColumnsGetFunc: JiraReleaseReportCSVColumn,
      widgetCSVDataTransform: JiraReleaseReportCSVDataTransformer
    },
    [GET_CUSTOMIZE_TITLE]: getJiraReleaseTitle,
    hasPaginationTableOnWidget: true,
    onChartClickPayload: onChartClickHandler,
    filters: {
      across: "fix_versions"
    },
    getDrilldownTitle: getJiraReleaseDrilldownTitle,
    getDrillDownType: getJiraReleaseDrilldownType,
    [CUSTOM_FIELD_KEY]: CUSTOM_FIELD_KEY,
    [WIDGET_MIN_HEIGHT]: "12rem"
  }
};

export default jiraReleaseTableReport;
