import { leadTimePhaseTransformer } from "custom-hooks/helpers";
import {
  GET_GRAPH_FILTERS,
  PREVIEW_DISABLED,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { jiraLeadTimeDrilldown } from "dashboard/constants/drilldown.constants";
import {
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  SHOW_SETTINGS_TAB,
  SHOW_METRICS_TAB,
  SHOW_AGGREGATIONS_TAB,
  CSV_DRILLDOWN_TRANSFORMER,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  PARTIAL_FILTER_MAPPING_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, issueLeadTimeFilterOptionsMapping } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING, leadTimeExcludeStageFilter } from "dashboard/constants/filterWithInfo.mapping";
import { leadTimeJiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { leadTimeCsvTransformer } from "dashboard/helpers/csv-transformers/leadTimeCsvTransformer";
import { ChartContainerType, transformLeadTimeStageReportPrevQuery } from "dashboard/helpers/helper";
import { LeadTimeByStageReportTypes } from "model/report/jira/lead_time_by_stage_report/leadTimeByStageReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import {
  LEAD_TIME_BY_STAGE_REPORT_CHART_PROPS,
  LEAD_TIME_BY_STAGE_REPORT_FILTER,
  LEAD_TIME_STAGE_DEFAULT_QUERY,
  LEAD_TIME_STAGE_REPORT_DESCRIPTION
} from "./constants";
import { IssueLeadTimeByStageReportFiltersConfig } from "./filters.config";
import { LEAD_TIME_VALUES_TO_FILTERS_KEY } from "../constant";
import { getDrilldownCheckBox, getFilters } from "./helper";
import LeadTimeByStageFooter from "shared-resources/containers/server-paginated-table/components/Table-Footer/Lead-time-stage-footer";
import { IntegrationTypes } from "constants/IntegrationTypes";

const leadTimeByStageReport: {
  lead_time_by_stage_report: LeadTimeByStageReportTypes;
} = {
  lead_time_by_stage_report: {
    name: "Issue Lead Time by Stage Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.LEAD_TIME_PHASE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: LEAD_TIME_BY_STAGE_REPORT_CHART_PROPS,
    dataKey: "duration",
    uri: "lead_time_report",
    description: LEAD_TIME_STAGE_REPORT_DESCRIPTION,
    method: "list",
    filters: LEAD_TIME_BY_STAGE_REPORT_FILTER,
    defaultAcross: "velocity",
    default_query: LEAD_TIME_STAGE_DEFAULT_QUERY,
    supportExcludeFilters: true,
    supportPartialStringFilters: true,
    supported_filters: leadTimeJiraSupportedFilters,
    jira_or_filter_key: "jira_or",
    [FILTER_NAME_MAPPING]: issueLeadTimeFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    [PREVIEW_DISABLED]: true,
    drilldown: jiraLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: (data: any) => leadTimePhaseTransformer(data),
    [API_BASED_FILTER]: ["jira_reporters", "jira_assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformLeadTimeStageReportPrevQuery(data),
    [REPORT_FILTERS_CONFIG]: IssueLeadTimeByStageReportFiltersConfig,
    valuesToFilters: LEAD_TIME_VALUES_TO_FILTERS_KEY,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [GET_GRAPH_FILTERS]: getFilters,
    includeContextFilter: true,
    drilldownFooter: () => LeadTimeByStageFooter,
    drilldownCheckbox: getDrilldownCheckBox,
    drilldownMissingAndOtherRatings: true,
    drilldownTotalColCaseChange: true
  }
};
export default leadTimeByStageReport;
