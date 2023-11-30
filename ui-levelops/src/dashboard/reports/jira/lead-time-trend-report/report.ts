import { leadTimeTrendTransformer } from "custom-hooks/helpers";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { jiraLeadTimeDrilldown } from "dashboard/constants/drilldown.constants";
import {
  SHOW_SETTINGS_TAB,
  SHOW_METRICS_TAB,
  SHOW_AGGREGATIONS_TAB,
  CSV_DRILLDOWN_TRANSFORMER,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  PARTIAL_FILTER_MAPPING_KEY,
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, issueLeadTimeFilterOptionsMapping } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING, leadTimeExcludeStageFilter } from "dashboard/constants/filterWithInfo.mapping";
import { leadTimeJiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { leadTimeCsvTransformer } from "dashboard/helpers/csv-transformers/leadTimeCsvTransformer";
import { ChartContainerType, transformLeadTimeReportPrevQuery } from "dashboard/helpers/helper";
import { IssueLeadTimeTrendReportFiltersConfig } from "./filters.config";
import { LeadTimeTrendReportTypes } from "model/report/jira/lead-time-trend-report/LeadTimeTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraLeadTimeDefaultQuery, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { leadTimeTrendReportChartProps, leadTimeTrendReportFilter } from "./constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING, LEAD_TIME_VALUES_TO_FILTERS_KEY } from "../constant";
import { REPORT_KEY_IS_ENABLED } from "../../constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

const leadTimeTrendReport: {
  lead_time_trend_report: LeadTimeTrendReportTypes;
} = {
  lead_time_trend_report: {
    [REPORT_KEY_IS_ENABLED]: false,
    name: "Issue Lead Time Trend Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: leadTimeTrendReportChartProps,
    uri: "lead_time_report",
    method: "list",
    filters: leadTimeTrendReportFilter,
    default_query: jiraLeadTimeDefaultQuery,
    convertTo: "days",
    [FILTER_NAME_MAPPING]: issueLeadTimeFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [SHOW_SETTINGS_TAB]: true,
    widget_height: "375px",
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    supported_filters: leadTimeJiraSupportedFilters,
    drilldown: jiraLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: (data: any) => leadTimeTrendTransformer(data),
    shouldJsonParseXAxis: () => true,
    [API_BASED_FILTER]: ["jira_reporters", "jira_assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformLeadTimeReportPrevQuery(data),
    [REPORT_FILTERS_CONFIG]: IssueLeadTimeTrendReportFiltersConfig,
    valuesToFilters: LEAD_TIME_VALUES_TO_FILTERS_KEY,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};
export default leadTimeTrendReport;
