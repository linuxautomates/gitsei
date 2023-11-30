import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  REQUIRED_ONE_FILTER,
  REQUIRED_ONE_FILTER_KEYS
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { handleRequiredForFilters } from "dashboard/reports/jira/commonJiraReports.helper";
import { requiredOneFiltersKeys } from "dashboard/reports/jira/constant";
import { AzureSprintImpactUnestimatedTicketReportType } from "model/report/azure/sprint-impact-unestimated-tickets/sprint-impact-unestimated-tickets.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { sprintImpactTransformer } from "transformers/reports/sprintMetricsPercentReportTransformer";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  COMMON_FILTER_OPTIONS_MAPPING,
  REPORT_LIST_METHOD
} from "../constant";
import { CHART_PROPS, DEFAULT_ACROSS, DEFAULT_QUERY, FILTERS, REPORT_NAME, URI } from "./constant";
import { SprintImpactOfUnestimatedTicketReportFiltersConfig } from "./filter.config";

const sprintImpactUnestimatedTicketsReport: {
  azure_sprint_impact_estimated_ticket_report: AzureSprintImpactUnestimatedTicketReportType;
} = {
  azure_sprint_impact_estimated_ticket_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: URI,
    method: REPORT_LIST_METHOD,
    defaultAcross: DEFAULT_ACROSS,
    filters: FILTERS,
    default_query: DEFAULT_QUERY,
    xaxis: false,
    chart_props: CHART_PROPS,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: {},
    transformFunction: sprintImpactTransformer,
    [HIDE_REPORT]: true,
    [FILTER_NAME_MAPPING]: COMMON_FILTER_OPTIONS_MAPPING,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: SprintImpactOfUnestimatedTicketReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer,
    [REQUIRED_ONE_FILTER]: (config: any, query: any, report: string) => handleRequiredForFilters(config, query, report),
    [REQUIRED_ONE_FILTER_KEYS]: requiredOneFiltersKeys
  }
};

export default sprintImpactUnestimatedTicketsReport;
