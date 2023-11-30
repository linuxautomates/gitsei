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
import { AzureFirstAssigneeReportType } from "model/report/azure/first-assignee-report/first-assignee-report.model";
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
  REPORT_NAME,
  SHOW_EXTRA_INFO_ON_TOOLTIP,
  SORT_KEY,
  URI
} from "./constant";
import { FirstAssigneeReportFiltersConfig } from "./filter.config";
import { xAxisLabeltransform } from "./helper";

const firstAssigneeReport: { azure_first_assignee_report: AzureFirstAssigneeReportType } = {
  azure_first_assignee_report: {
    name: REPORT_NAME,
    application: ADO_APPLICATION,
    chart_type: ChartType?.BAR,
    defaultAcross: DEFAULT_ACROSS,
    across: ACROSS_OPTIONS.map((item: { label: string; value: string }) => item.value),
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: DEFAULT_FILTER_KEY,
    showExtraInfoOnToolTip: SHOW_EXTRA_INFO_ON_TOOLTIP,
    xaxis: true,
    chart_props: CHART_PROPS,
    uri: URI,
    method: REPORT_LIST_METHOD,
    filters: {},
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: SORT_KEY,
    default_query: DEFAULT_QUERY,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    supported_filters: issueManagementSupportedFilters,
    convertTo: "days",
    drilldown: azureDrilldown,
    transformFunction: seriesDataTransformer,
    [FILTER_NAME_MAPPING]: issueManagementCommonFilterOptionsMapping,
    [HIDE_REPORT]: true,
    xAxisLabelTransform: xAxisLabeltransform,
    onChartClickPayload: onChartClickPayloadForId,
    [REPORT_FILTERS_CONFIG]: FirstAssigneeReportFiltersConfig,
    valuesToFilters: drillDownValuesToFiltersKeys,
    [API_BASED_FILTER]: AZURE_API_BASED_FILTERS,
    [FIELD_KEY_FOR_FILTERS]: AZURE_API_BASED_FILTER_KEY_MAPPING,
    [PREV_REPORT_TRANSFORMER]: azureCommonPrevQueryTansformer
  }
};

export default firstAssigneeReport;
