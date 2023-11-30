import { leadTimeTypeTransformer } from "custom-hooks/helpers";
import {
  PREVIEW_DISABLED,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { jiraLeadTimeDrilldown } from "dashboard/constants/drilldown.constants";
import {
  SHOW_SETTINGS_TAB,
  SHOW_METRICS_TAB,
  SHOW_AGGREGATIONS_TAB,
  CSV_DRILLDOWN_TRANSFORMER,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, issueLeadTimeFilterOptionsMapping } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING, leadTimeExcludeStageFilter } from "dashboard/constants/filterWithInfo.mapping";
import { leadTimeJiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { leadTimeCsvTransformer } from "dashboard/helpers/csv-transformers/leadTimeCsvTransformer";
import { ChartContainerType, transformLeadTimeReportPrevQuery } from "dashboard/helpers/helper";
import { LeadTimeByTypeReportTypes } from "model/report/jira/lead_time_by_type_report/leadTimeByTypeReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraLeadTimeDefaultQuery, jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { leadTimeByTypeReportFilter } from "./constants";
import { leadTimeByTypeReportChartProps } from "./constants";
import { IssueLeadTimeByTypeReportFiltersConfig } from "./filters.config";
import { LEAD_TIME_VALUES_TO_FILTERS_KEY } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const leadTimeByTypeReport: {
  lead_time_by_type_report: LeadTimeByTypeReportTypes;
} = {
  lead_time_by_type_report: {
    name: "Issue Lead Time by Type Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.LEAD_TIME_TYPE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    // xaxis: true,  TODO: Add later, Out of scope for initial release
    chart_props: leadTimeByTypeReportChartProps,
    uri: "lead_time_report",
    method: "list",
    filters: leadTimeByTypeReportFilter,
    defaultAcross: "velocity",
    default_query: jiraLeadTimeDefaultQuery,
    supported_filters: leadTimeJiraSupportedFilters,
    [FILTER_NAME_MAPPING]: issueLeadTimeFilterOptionsMapping,
    [FILTER_WITH_INFO_MAPPING]: [leadTimeExcludeStageFilter],
    [SHOW_SETTINGS_TAB]: true,
    [SHOW_METRICS_TAB]: true,
    [SHOW_AGGREGATIONS_TAB]: false,
    [PREVIEW_DISABLED]: true,
    drilldown: jiraLeadTimeDrilldown,
    [CSV_DRILLDOWN_TRANSFORMER]: leadTimeCsvTransformer,
    transformFunction: (data: any) => leadTimeTypeTransformer(data),
    shouldJsonParseXAxis: () => true,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformLeadTimeReportPrevQuery(data),
    [API_BASED_FILTER]: ["jira_reporters", "jira_assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: IssueLeadTimeByTypeReportFiltersConfig,
    valuesToFilters: LEAD_TIME_VALUES_TO_FILTERS_KEY,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};
export default leadTimeByTypeReport;
