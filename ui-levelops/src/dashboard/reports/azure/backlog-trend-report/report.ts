import { ChartType } from "../../../../shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "../../../helpers/helper";
import { getXAxisLabel } from "../../../../shared-resources/charts/bar-chart/bar-chart.helper";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  HIDE_REPORT,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "../../../constants/filter-key.mapping";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  issueManagementCommonFilterOptionsMapping
} from "../../../constants/filter-name.mapping";
import { issueManagementSupportedFilters } from "../../../constants/supported-filters.constant";
import { azureBacklogDrillDown } from "../../../constants/drilldown.constants";
import { azureBacklogTransformerWrapper } from "../../../../custom-hooks/helpers/helper";
import {
  MULTI_SERIES_REPORT_FILTERS_CONFIG,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "../../../constants/applications/names";
import { AzureBacklogTrendReportType } from "../../../../model/report/azure/backlog-trend-report/backlog-trend-report.model";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  AZURE_API_BASED_FILTERS,
  drillDownValuesToFiltersKeys,
  REPORT_LIST_METHOD
} from "../constant";
import { OnChartClickPayload } from "./helper";
import { IssueBacklogTrendReportFiltersConfig } from "./filter.config";
import { CHART_PROPS, DEFAULT_QUERY, FILTERS, REPORT_NAME, STACKS_FILTERS, URI } from "./constant";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { AzureMultiSeriesBacklogTrendReportFiltersConfig } from "dashboard/reports/multiseries-reports/azure/backlog-trend-report/filter-config";

const backlogTrendReport: { azure_backlog_trend_report: AzureBacklogTrendReportType } = {
  azure_backlog_trend_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: CHART_PROPS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: FILTERS,
    stack_filters: STACKS_FILTERS,
    default_query: DEFAULT_QUERY,
    xAxisLabelTransform: getXAxisLabel,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureBacklogDrillDown,
    shouldReverseApiData: () => false,
    transformFunction: azureBacklogTransformerWrapper,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: IssueBacklogTrendReportFiltersConfig,
    onChartClickPayload: OnChartClickPayload,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer,
    [MULTI_SERIES_REPORT_FILTERS_CONFIG]: AzureMultiSeriesBacklogTrendReportFiltersConfig
  }
};

export default backlogTrendReport;
