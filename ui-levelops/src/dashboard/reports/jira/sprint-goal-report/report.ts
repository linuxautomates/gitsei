import { ChartContainerType } from "dashboard/helpers/helper";
import { HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { sprintGoalReportColumns } from "dashboard/pages/dashboard-tickets/configs/sprintSingleStatTableConfig";
import { modificationMappedValues } from "dashboard/graph-filters/components/helper";
import { jiraSprintGoalSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { genericDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import { tableTransformer } from "custom-hooks/helpers";
import {
  DEFAULT_METADATA,
  FILTER_KEY_MAPPING,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  SHOW_METRICS_TAB
} from "dashboard/constants/filter-key.mapping";
import { SprintGoalReportType } from "model/report/jira/sprint-goal-report/sprintGoalReport.constant";
import { JiraSprintGoalReportFiltersConfig } from "./filters.config";
import { FILTER_NAME_MAPPING } from "../../../constants/filter-name.mapping";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { COMPLETED_DATE_OPTIONS, DEFAULT_META } from "./constant";
import { WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS, WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const sprintGoalReport: { sprint_goal: SprintGoalReportType } = {
  sprint_goal: {
    name: "Sprint Goal Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      size: "small",
      columns: sprintGoalReportColumns,
      chartProps: {}
    },
    uri: "jira_sprint_filters",
    method: "list",
    filters: {},
    default_query: {
      completed_at: modificationMappedValues("last_month", COMPLETED_DATE_OPTIONS)
    },
    supported_filters: jiraSprintGoalSupportedFilters,
    drilldown: {
      allowDrilldown: true,
      title: "Sprint Goals",
      uri: "jira_sprint_filters",
      application: "sprint_goal",
      columns: sprintGoalReportColumns,
      supported_filters: jiraSprintGoalSupportedFilters,
      drilldownTransformFunction: data => genericDrilldownTransformer(data)
    },
    transformFunction: data => tableTransformer(data),
    [FILTER_KEY_MAPPING]: {
      sprint: "name"
    },
    [DEFAULT_METADATA]: DEFAULT_META,
    [REPORT_FILTERS_CONFIG]: JiraSprintGoalReportFiltersConfig,
    [SHOW_METRICS_TAB]: false,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [HIDE_CUSTOM_FIELDS]: true,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS
  }
};

export default sprintGoalReport;
