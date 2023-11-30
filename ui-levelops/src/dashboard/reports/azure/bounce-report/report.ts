import { bounceReportTransformer } from "custom-hooks/helpers";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "dashboard/constants/filter-key.mapping";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping,
  VALUE_SORT_KEY
} from "dashboard/constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { AzureBounceReportType } from "model/report/azure/bounce-report/bounce-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  drillDownValuesToFiltersKeys,
  REPORT_LIST_METHOD
} from "../constant";
import {
  ACROSS_OPTIONS,
  CHART_PROPS,
  DEFAULT_ACROSS,
  DEFAULT_QUERY,
  DEFAULT_SORT,
  DRILL_DOWN,
  REPORT_NAME,
  SORT_KEY,
  URI
} from "./constant";
import { BounceReportFiltersConfig } from "./filter.config";
import { onChartClickHandler, xAxisLabelTransform } from "./helper";

const bounceReport: { azure_bounce_report: AzureBounceReportType } = {
  azure_bounce_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    //added for backword compatibility, we need to remove when all reports work with the new flow
    across: ACROSS_OPTIONS.map((option: { label: string; value: string }) => option?.value),
    xaxis: true,
    defaultAcross: DEFAULT_ACROSS,
    chart_type: ChartType?.SCATTER,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: CHART_PROPS,
    defaultSort: DEFAULT_SORT,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: SORT_KEY,
    [HIDE_REPORT]: true,
    default_query: DEFAULT_QUERY,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    drilldown: DRILL_DOWN,
    transformFunction: bounceReportTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [REPORT_FILTERS_CONFIG]: BounceReportFiltersConfig,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    xAxisLabelTransform: xAxisLabelTransform,
    onChartClickPayload: onChartClickHandler,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};

export default bounceReport;
