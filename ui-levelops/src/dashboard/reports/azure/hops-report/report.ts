import { seriesDataTransformer } from "custom-hooks/helpers";
import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { azureDrilldown } from "dashboard/constants/drilldown.constants";
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
import { AzureHopsReportType } from "model/report/azure/hops-report/hops-report.model";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  ADO_APPLICATION,
  AZURE_API_BASED_FILTERS,
  AZURE_API_BASED_FILTER_KEY_MAPPING,
  drillDownValuesToFiltersKeys,
  REPORT_LIST_METHOD
} from "../constant";
import { onChartClickPayloadForId } from "../helpers/common-onchartClick";
import {
  ACROSS_OPTIONS,
  CHART_PROPS,
  DEFAULT_ACROSS,
  DEFAULT_FILTER_KEY,
  DEFAULT_QUERY,
  DEFAULT_SORT,
  REPORT_NAME,
  SHOW_EXTRA_INFO_ON_TOOLTIP,
  SORT_KEY,
  URI
} from "./constant";
import { IssueHopsReportFiltersConfig } from "./filter.config";
import { xAxisLabelTransform } from "./helper";

const hopsReport: { azure_hops_report: AzureHopsReportType } = {
  azure_hops_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    defaultAcross: DEFAULT_ACROSS,
    across: ACROSS_OPTIONS.map((item: { label: string; value: string }) => item.value),
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: DEFAULT_FILTER_KEY,
    showExtraInfoOnToolTip: SHOW_EXTRA_INFO_ON_TOOLTIP,
    xaxis: true,
    default_query: DEFAULT_QUERY,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: SORT_KEY,
    defaultSort: DEFAULT_SORT,
    chart_props: CHART_PROPS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [HIDE_REPORT]: true,
    supported_filters: issueManagementSupportedFilters,
    drilldown: azureDrilldown,
    transformFunction: seriesDataTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    valuesToFilters: drillDownValuesToFiltersKeys,
    xAxisLabelTransform: xAxisLabelTransform,
    onChartClickPayload: onChartClickPayloadForId,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [REPORT_FILTERS_CONFIG]: IssueHopsReportFiltersConfig,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};

export default hopsReport;
