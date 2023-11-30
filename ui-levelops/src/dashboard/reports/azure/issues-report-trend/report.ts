import { azureTrendTransformer } from "custom-hooks/helpers/trendReport.helper";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { azureDrilldown } from "dashboard/constants/drilldown.constants";
import {
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  HIDE_REPORT,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS
} from "dashboard/constants/filter-key.mapping";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping
} from "dashboard/constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { AzureTicketsTrendReportType } from "model/report/azure/tickets-report-trend/tickets-report-trend.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  drillDownValuesToFiltersKeys,
  ADO_APPLICATION,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_API_BASED_FILTERS,
  REPORT_LIST_METHOD
} from "../constant";
import { CHART_PROPS, COMPOSITE_TRANSFORM, FILTERS, REPORT_NAME, URI } from "./constants";
import { IssueReportTrendsFiltersConfig } from "./filter.config";

const issuesTrendReport: { azure_tickets_report_trends: AzureTicketsTrendReportType } = {
  azure_tickets_report_trends: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: COMPOSITE_TRANSFORM,
    chart_props: CHART_PROPS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    transformFunction: azureTrendTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer,
    [REPORT_FILTERS_CONFIG]: IssueReportTrendsFiltersConfig
  }
};

export default issuesTrendReport;
